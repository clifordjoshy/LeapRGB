#include <WebSocketServer.h>
#include<FastLED.h>
#include<ESP8266WiFi.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#define get_var(v) pgm_read_byte(v)

//FILL ALL OF THESE VARIABLES ACCORDING TO SETUP
const char *MYSSID = "", *PASSWORD = "", *SUB_DELIM = "/", *MAIN_DELIM = "&";
const int DATAPIN = 5, SWITCHPIN = D2;
const int COLUMNCOUNT = 16, ROWCOUNT=15, LEDCOUNT = ROWCOUNT*COLUMNCOUNT;
const bool isConnectedAtTop = false, isConnectedAtLeft = true ;

const int LEDBRIGHTNESS = 10, PRINT_DELAY = 150;

/*
 NOTES
 * PASS THE HEXCODE WITHOUT '#' I.E ONLY THE HEX VALUE
 * DO THE WIRING ROWWISE ONLY
 * Defined Messages = [setlight, setoff, show, clear, clearrow, delay, render, visualize]
 * Format :- Call/Row/Column/Hex     (as needed)
*/

//FUNCTIONS
int handleCoordinates(int Row, int Column);
void clearAll();
bool switchUpdateCheck();

byte* getCharacterImage(char c);
int printToDisplay(byte* image, const int PRINT_ROW, const int PRINT_COLUMN, CRGB PRINT_COLOR);
void shiftLeft();

void clockModeMain();
void drawClock();
void printDayOfWeek(int day, const int PRINT_ROW, int PRINT_COLUMN, const CRGB colorScheme[2]);

void appModeMain();
void drawApp();
void printIP(IPAddress ip);
void setLight(char *token_ptr);
void setOff(char *token_ptr);
void clearRow(char *token_ptr);
void doDelay(char *token_ptr);
void renderText(char *token_ptr); 
int doSpacing(int currentColumn);
void startVisualizer();

void doAnimation1();
void doAnimation2();
void drawBox(int rowup, int rowdown, int colleft, int colright, CRGB color);

//GLOBAL VARIABLES 
const int MODE_APP = 0, MODE_CLOCK = 1;
const int CHAR_WIDTH = 3, CHAR_HEIGHT = 5;
int currentMode = MODE_CLOCK;
CRGB leds[LEDCOUNT];
WebSocketServer webSocketServer;

void setup() {
	Serial.begin(115200);
	FastLED.addLeds<WS2811, DATAPIN>(leds, LEDCOUNT);
	FastLED.setBrightness(LEDBRIGHTNESS); 
	
	pinMode(SWITCHPIN, INPUT_PULLUP);
	
	connectToWifi();
}

void loop(){
	if(WiFi.status() != WL_CONNECTED){	//if connection lost
		clearAll();
		connectToWifi();
	}
		
	switch(currentMode){
		case MODE_APP:
			appModeMain();
			break;
		case MODE_CLOCK:
			clockModeMain();
			break;
	}
	
	for(int i=0; i < COLUMNCOUNT; ++i){ 
		shiftLeft();
		FastLED.show();
		delay(5);
	}
}

void connectToWifi(){
	WiFi.begin(MYSSID, PASSWORD);

	int i; 
	for(i = 0; WiFi.status() != WL_CONNECTED; ++i){
		//Not Connected Indication
		leds[i]=CRGB::Red;
		FastLED.show();
		delay(500);
	}
 
	Serial.println(WiFi.localIP());
  
	//Connected Indication
	for(i = i-1; i >= 0; --i)
		leds[i]=CRGB::Green;
	FastLED.show();
	delay(500);   

	clearAll();
}

void clockModeMain(){

	drawClock();
	if(currentMode != MODE_CLOCK){   //4 second window
		//button has been pressed
		return;
	}

	clearAll();
    
	const int UTC_OFFSET_MS = 5.5 * 60 * 60;		//timezone [UTC + 5:30]
	const int TIMEROW = 2, DAYROW = 10, COLUMN_DAY = 1, COLUMN_DATE[2] = {10, 13}, COLUMN_HOURS[2] = {1, 4}, COLUMN_COLON = 8, COLUMN_MINUTES[2] = {10,13};
	const CRGB colorTime[] = {CRGB::Orange, CRGB::Blue}, colorTimeSub = CRGB::Chartreuse; 
	bool isColonVisible = true;
	
	WiFiUDP ntpUDP;
	NTPClient timeClient(ntpUDP, "pool.ntp.org", UTC_OFFSET_MS);
	
	int timeDigits[4] = {-1, -1, -1, -1}, dayDigit = -1, dateDigits[2];
	String date = "";
	timeClient.begin();
	
	printToDisplay(getCharacterImage(':'), TIMEROW, COLUMN_COLON, colorTimeSub);
	int i;
	for(i = 1;i <= COLUMNCOUNT; ++i)
		leds[handleCoordinates(DAYROW-2, i)] = colorTimeSub;
	for(i = DAYROW-1; i <= ROWCOUNT; ++i)
		leds[handleCoordinates(i, COLUMN_COLON)] = colorTimeSub;
	
	while(currentMode == MODE_CLOCK){
		timeClient.update();
		if(timeDigits[0] != timeClient.getHours() / 10){			
			timeDigits[0] = timeClient.getHours()/10;      
			printToDisplay(getCharacterImage(timeDigits[0]), TIMEROW, COLUMN_HOURS[0], colorTime[0]);
		}
		
		if(timeDigits[1] != timeClient.getHours() % 10){	
			timeDigits[1] = timeClient.getHours()%10;
			printToDisplay(getCharacterImage(timeDigits[1]), TIMEROW, COLUMN_HOURS[1], colorTime[1]);
		}
	
		if(timeDigits[2] != timeClient.getMinutes() / 10){
			timeDigits[2] = timeClient.getMinutes()/10;
			printToDisplay(getCharacterImage(timeDigits[2]), TIMEROW, COLUMN_MINUTES[0], colorTime[0]);
		}
		
		if(timeDigits[3] != timeClient.getMinutes() % 10){		
			timeDigits[3] = timeClient.getMinutes()%10;
			printToDisplay(getCharacterImage(timeDigits[3]), TIMEROW, COLUMN_MINUTES[1], colorTime[1]);
		}
		
		if(dayDigit != timeClient.getDay()){
			dayDigit = timeClient.getDay();
			printDayOfWeek(dayDigit, DAYROW, COLUMN_DAY, colorTime);
		}
		
		//YYYY-MM-DDT.....
		if(!date.equals(timeClient.getFormattedDate().substring(8, 10))){
			date = timeClient.getFormattedDate().substring(8, 10);
			int dateInt = date.toInt();
			dateDigits[0] = dateInt/10;
			dateDigits[1] = dateInt%10;
			printToDisplay(getCharacterImage(dateDigits[0]), DAYROW, COLUMN_DATE[0], colorTime[0]);
			printToDisplay(getCharacterImage(dateDigits[1]), DAYROW, COLUMN_DATE[1], colorTime[1]);
		}
		
		delay(1000);		
		switchUpdateCheck();
	}
}

void appModeMain() {
  
	drawApp();
	if(currentMode != MODE_APP){   //4 second window
		//button has been pressed
		return;
	}

	clearAll();
  
	WiFiServer server(80);
	WiFiClient client;
	
	server.begin();
	
	int notConnectedIterations = 0;
	
	//loops simply until a client is connected
	while(currentMode == MODE_APP){
		client = server.available();
    
		//establish websocket handshake
		if (client.connected() && webSocketServer.handshake(client)) {
			Serial.println("Connected to Client");
			notConnectedIterations = 0;

			// Communication with the client
			String dataString;
      		char *save_token_ptr;
			char dataArray[300];
			
			while(client.connected()){
				//Receive Data From Client
				dataString = webSocketServer.getData(); 
		  
				if (dataString.length() > 0) {
					//Handle the Received Data
					dataString.toCharArray(dataArray, 300);
					
					char* token = strtok_r(dataArray, MAIN_DELIM, &save_token_ptr);
					
					while (token != NULL) {

						char *save_ptr_2;   ///to continue extracting from string
						char* toCall = strtok_r(token, SUB_DELIM, &save_ptr_2);
						Serial.println(toCall);
						if(strcmp(toCall, "setlight") == 0)
							setLight(save_ptr_2);
						else if(strcmp(toCall, "setoff") == 0)
							setOff(save_ptr_2);
						else if(strcmp(toCall, "show") == 0)
							FastLED.show();
						else if(strcmp(toCall, "clear") == 0)
							clearAll();
						else if(strcmp(toCall, "clearrow") == 0)
							clearRow(save_ptr_2);
						else if(strcmp(toCall, "delay") == 0)
							doDelay(save_ptr_2);
						else if(strcmp(toCall, "render") == 0)
							renderText(save_ptr_2);
						else if(strcmp(toCall, "visualize") == 0)
							startVisualizer();
						else if(strcmp(toCall, "animation1") == 0)
							doAnimation1();
						else if(strcmp(toCall, "animation2") == 0)
							doAnimation2();

             token = strtok_r(NULL, MAIN_DELIM, &save_token_ptr);
					} 
				}
				delay(5); //delay for receiving data
				if(switchUpdateCheck()){
					webSocketServer.disconnectStream();
					break;
				}
			}
			Serial.println("Client Disconnected");
			delay(100);
		}
		
		if(switchUpdateCheck())
			break;
		
		++notConnectedIterations;
		if(notConnectedIterations > 150){
			printIP(WiFi.localIP());
			delay(PRINT_DELAY);
			clearAll();
			notConnectedIterations = 0;
		}   
		delay(100);
	}
  server.close();
}

//Receives subsequent messages and visualizes until 'stop' is received
void startVisualizer(){
	CRGB visualizerColors[COLUMNCOUNT] = {CRGB::White, CRGB::Indigo, CRGB::Blue, CRGB::Green, CRGB::Yellow, 
		CRGB::Orange, CRGB::Red, CRGB::Magenta, CRGB::White, CRGB::Indigo, CRGB::Blue, CRGB::Green,
		CRGB::Yellow, CRGB::Orange, CRGB::Red, CRGB ::Magenta};
	String dataString;
	char dataArray[300], *token_ptr, *token;

	while(true){
		dataString = webSocketServer.getData();
		if (dataString.length() == 0)
			continue;
		dataString.toCharArray(dataArray, 300);
		if(strcmp("stop", dataArray) == 0)
			break;
		token = strtok_r(dataArray, SUB_DELIM, &token_ptr);
		for(int i = 1; i <= COLUMNCOUNT; ++i){
			int bar_h = ROWCOUNT - atoi(token) + 1;
			int j;
			for(j = ROWCOUNT; j >= bar_h; --j)
				leds[handleCoordinates(j, i)] = visualizerColors[i-1];
			for(;j > 0; --j)
				leds[handleCoordinates(j, i)] = CRGB::Black;
			token = strtok_r(NULL, SUB_DELIM, &token_ptr);
		}
		
		FastLED.show();
    }
}

//Renders text in specified color
void renderText(char *token_ptr){
	const int TEXT_PRINTROW = 6;
	char *text = strtok_r(NULL, SUB_DELIM, &token_ptr);
	int hex = strtol(strtok_r(NULL, SUB_DELIM, &token_ptr), NULL, 16);  //base 16 conversion
	CRGB color = hex;

  while(true){
    int currentColumn = 1;
    char c = text[0];
  	for(int i = 1; c != '\0'; c = text[i++]){
  		if (c == ' '){
        FastLED.show();
        delay(PRINT_DELAY);
        for(int r = 0; r < 3; ++r){
    			currentColumn = doSpacing(currentColumn);
          FastLED.show();
          delay(PRINT_DELAY);
        }
  			continue;
  		}
  		int filled_lines = printToDisplay(getCharacterImage(c), TEXT_PRINTROW, currentColumn, color);
  		currentColumn += filled_lines;
  		currentColumn = doSpacing(currentColumn);
  	}
   
    while(currentColumn > 1){
      shiftLeft();
      --currentColumn;
      FastLED.show();
      delay(PRINT_DELAY);
    }

    String dataString = webSocketServer.getData();
      if(dataString.length() > 0) //if any thing is received
        break;
  }
}

//Handles delay
void doDelay(char *token_ptr){
	char* delay_chars = strtok_r(NULL, SUB_DELIM, &token_ptr);
	int delay_val = atoi(delay_chars);
	delay(delay_val);
}

//Receives the coordinate and color to light up an LED
void setLight(char *token_ptr) {
	int row, column, hex;
	char *dataValue;
  
	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	row = atoi(dataValue);
  
	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	column = atoi(dataValue);
  
	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	hex = strtol(dataValue, NULL, 16);  //base 16 conversion
  
	int ledno = handleCoordinates(row, column);
	leds[ledno] = hex;
}

//Sets the LED at some coordinate to off
void setOff(char *token_ptr) {
	int row, column;
	char *dataValue;

	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	row = atoi(dataValue);
  
	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	column = atoi(dataValue);
   
	int ledno = handleCoordinates(row, column);
	leds[ledno] = CRGB::Black;
}

//Handles the Row and Column according to the wiring. Needed because wiring is L->R R->L .... [indexed from 1]
int handleCoordinates(int row, int column){
	int ledno;
 
	if(isConnectedAtTop && isConnectedAtLeft){  //TL
		if(row%2)   //Odd Row i.e L->R row
			ledno = (row - 1) * COLUMNCOUNT + column - 1;
		else        //Even Row i.e R->L row
			ledno = row * COLUMNCOUNT - column;
	}

	else if(isConnectedAtTop && !isConnectedAtLeft){  //TR
		if(row%2)   //Odd Row i.e L->R row
			ledno = row * COLUMNCOUNT - column;
		else        //Even Row i.e R->L row
			ledno = (row - 1) * COLUMNCOUNT + column - 1;
    }

	else if(!isConnectedAtTop && !isConnectedAtLeft){ //BR
		if(row%2)   //Odd Row i.e R->L row
			ledno = (ROWCOUNT - row) * COLUMNCOUNT + (COLUMNCOUNT - column);
		else        //Even Row i.e L->R row
			ledno = (ROWCOUNT - row) * COLUMNCOUNT + column - 1;
    }

	else{ //BL
		if(row%2)   //Odd Row i.e L->R row
			ledno = (ROWCOUNT - row) * COLUMNCOUNT + column - 1;
		else        //Even Row i.e R->L row
			ledno = (ROWCOUNT - row) * COLUMNCOUNT + (COLUMNCOUNT - column);  
    }
	
	return ledno;    
}

//Turns off all the LEDs
void clearAll(){
	for(int i = 0; i < LEDCOUNT; ++i)
		leds[i] = CRGB::Black;
	FastLED.show();
	Serial.println("All cleared");
}

//Clears a row between specified columns
void clearRow(char *token_ptr){
	int row, columnStart, columnEnd;
	char *dataValue;
  
	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	row = atoi(dataValue);
	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	columnStart = atoi(dataValue);
	dataValue = strtok_r(NULL, SUB_DELIM, &token_ptr);
	columnEnd = atoi(dataValue);

	for(int i = columnStart; i <= columnEnd; ++i)
		leds[handleCoordinates(row, i)] = CRGB::Black;
}

void drawBox(int rowup, int rowdown, int colleft, int colright, CRGB color){
	int i;
	for(i = colleft; i <= colright; ++i){
		leds[handleCoordinates(rowup,i)] = color;
		leds[handleCoordinates(rowdown,i)] = color;
    }
	
	for(i = rowup + 1; i < rowdown; ++i){
		leds[handleCoordinates(i,colleft)] = color;
		leds[handleCoordinates(i,colright)] = color;
    }
}

void printIP(IPAddress ip){
	const int IP_PRINTROW = 6;
	const CRGB COLOR_IP = CRGB::White;

	int currentColumn = 1;

	clearAll();
  
	int i;
	for(i = 0; i < 4; ++i){ //ip is 4 integers
		int ipno = ip[i];
		int temp = 0;
		//reverse the number before passing to get digits
		do{
			temp = temp*10 + ipno%10;
			ipno = ipno/10;
		}while(ipno);

		ipno = temp;
    
		while(ipno){
			int digit = ipno%10;
			printToDisplay(getCharacterImage(digit), IP_PRINTROW, currentColumn, COLOR_IP);
			currentColumn += CHAR_WIDTH;
      		ipno = ipno / 10;
			currentColumn = doSpacing(currentColumn);
			
		}
		
		if(i<3){  //no dot at the end
			printToDisplay(getCharacterImage('.'), IP_PRINTROW, currentColumn, COLOR_IP);
			++currentColumn;
			currentColumn = doSpacing(currentColumn);
		}
	}
	delay(2 * PRINT_DELAY);
}

int doSpacing(int currentColumn){
	if(currentColumn > COLUMNCOUNT){
				shiftLeft();
				shiftLeft();  
				currentColumn = COLUMNCOUNT;
	} else if (currentColumn == COLUMNCOUNT){
		shiftLeft();  
	} else{ 
		++currentColumn;
	}
	return currentColumn;
}

void printDayOfWeek(int day, const int PRINT_ROW, int PRINT_COLUMN, const CRGB colorScheme[2]){

	switch(day){
		case 0:	//sunday
			printToDisplay(getCharacterImage('S'), PRINT_ROW, PRINT_COLUMN, colorScheme[0]);
			printToDisplay(getCharacterImage('u'), PRINT_ROW, PRINT_COLUMN + 3, colorScheme[1]);
			break;
		case 1:	//monday 
			printToDisplay(getCharacterImage('M'), PRINT_ROW, PRINT_COLUMN, colorScheme[0]);
			printToDisplay(getCharacterImage('o'), PRINT_ROW, PRINT_COLUMN + 3, colorScheme[1]);
			break;
		case 2:	//tuesday 
			printToDisplay(getCharacterImage('T'), PRINT_ROW, PRINT_COLUMN, colorScheme[0]);
			printToDisplay(getCharacterImage('u'), PRINT_ROW, PRINT_COLUMN + 3, colorScheme[1]);
			break;
		case 3:	//wednesday 
			printToDisplay(getCharacterImage('W'), PRINT_ROW, PRINT_COLUMN, colorScheme[0]);
			printToDisplay(getCharacterImage('d'), PRINT_ROW, PRINT_COLUMN + 3, colorScheme[1]);
			break;
		case 4:	//thursday 
			printToDisplay(getCharacterImage('T'), PRINT_ROW, PRINT_COLUMN, colorScheme[0]);
			printToDisplay(getCharacterImage('h'), PRINT_ROW, PRINT_COLUMN + 3, colorScheme[1]);
			break;
		case 5:	//friday
			printToDisplay(getCharacterImage('F'), PRINT_ROW, PRINT_COLUMN, colorScheme[0]);
			printToDisplay(getCharacterImage('r'), PRINT_ROW, PRINT_COLUMN + 3, colorScheme[1]);
			break;
		case 6:	//saturday 
			printToDisplay(getCharacterImage('S'), PRINT_ROW, PRINT_COLUMN, colorScheme[0]);
			printToDisplay(getCharacterImage('a'), PRINT_ROW, PRINT_COLUMN + 3, colorScheme[1]);
			break;
	}
}
    
int printToDisplay(byte* image, const int PRINT_ROW, const int PRINT_COLUMN, CRGB PRINT_COLOR){
	int filled_lines = 0, printing_column = PRINT_COLUMN;
	int i, j;
	for (j = 0; j < CHAR_WIDTH; ++j){
		bool emptyLine = true;

		for(i = 0; i < CHAR_HEIGHT; ++ i){
			if (get_var(image + i * CHAR_WIDTH + j) == 1){
				if(emptyLine){
					if (printing_column > COLUMNCOUNT){
						shiftLeft();
						printing_column = COLUMNCOUNT;
					}
				emptyLine = false;
				//clear line
				for (int clear_pixel = 0; clear_pixel < CHAR_HEIGHT; ++clear_pixel)
					leds[handleCoordinates(PRINT_ROW + clear_pixel, printing_column)] = CRGB::Black;
				}
				
				leds[handleCoordinates(PRINT_ROW + i, printing_column)] = PRINT_COLOR;
			}
		}

		if(!emptyLine){
			++filled_lines;
			++printing_column;
			FastLED.show();
			delay(PRINT_DELAY);
		}
	}
	return filled_lines;
}

void shiftLeft(){
	int i, j;
	for(i = 1; i < COLUMNCOUNT; ++i){
		for(j = 1; j <= ROWCOUNT; ++j)
			leds[handleCoordinates(j, i)] = leds[handleCoordinates(j, i + 1)];
	}

	for(j = 1; j <= ROWCOUNT; ++j)
		leds[handleCoordinates(j, COLUMNCOUNT)] = CRGB::Black;
}  
  
void doAnimation1(){
	CRGB colors[8]={CRGB::White, CRGB::Violet, CRGB::Indigo, CRGB::Blue, CRGB::Green, CRGB::Yellow, CRGB::Orange, CRGB::Red};
	const int rowup=8, rowdown=8, colleft=8, colright=9;
	int i;
	String dataString;
	while(true){
		dataString = webSocketServer.getData();
		if(dataString.length() > 0) //if any thing is received
			break;
		for(i = 0; i <= 7; ++i){
			drawBox(rowup - i, rowdown + i, colleft - i, colright + i, colors[i]);
			FastLED.show();
			delay(50);

			drawBox(rowup - i, rowdown + i, colleft - i, colright + i, CRGB::Black);
		}
		for(i = 6; i >= 0; --i){
			drawBox(rowup - i, rowdown + i, colleft - i, colright + i, colors[i]);
			FastLED.show();
			delay(50);
			drawBox(rowup - i, rowdown + i, colleft - i, colright + i, CRGB::Black);
		}
	}
	FastLED.show();		//to show the removal of middle block to black
}

void doAnimation2(){
	CRGB colors[8]={CRGB::White, CRGB::Violet, CRGB::Indigo, CRGB::Blue, CRGB::Green, CRGB::Yellow, CRGB::Orange, CRGB::Red};
	int rowup = 8,rowdown = 8,colleft = 8,colright = 9;
	int i;
	String dataString;
	
	while(true){
		dataString = webSocketServer.getData();
		if(dataString.length() > 0) //if any thing is received
			break;
      
		for(i=0;i<=7;++i){
			drawBox(rowup-i,rowdown+i,colleft-i,colright+i,colors[i]);
			FastLED.show();
			delay(50);
		}
		
		delay(100);
		
		for(i=7; i>=0;--i){
			drawBox(rowup-i,rowdown+i,colleft-i,colright+i,CRGB::Black);
			FastLED.show();
			delay(50);
		}
    }
}

bool switchUpdateCheck(){
	if(digitalRead(SWITCHPIN) == 0){
		if(currentMode == MODE_APP)
			currentMode = MODE_CLOCK;
		else if(currentMode == MODE_CLOCK)
			currentMode = MODE_APP;
		
		return true;
	}
	return false;
}


const byte clock_circle[][2] PROGMEM = {{2, 5}, {2, 6}, {2, 7}, {2, 8}, {2, 9}, {2, 10}, {2, 11}, {3, 4}, {3, 5}, {3, 11}, {3, 12}, 
	{4, 3}, {4, 4}, {4, 12}, {4, 13}, {5, 2}, {5, 3}, {5, 13}, {5, 14}, {6, 2}, {6, 14}, {7, 2}, {7, 14}, {8, 2}, {8, 14}, {9, 2}, 
	{9,14}, {10, 2}, {10, 14}, {11, 2}, {11, 3}, {11, 13}, {11, 14}, {12, 3}, {12, 4}, {12, 12}, {12, 13}, {13, 4}, {13, 5}, 
	{13, 11}, {13, 12}, {14, 5}, {14, 6}, {14, 7}, {14, 8}, {14 ,9}, {14, 10}, {14, 11}};
const byte clock_markings[][2] PROGMEM = {{3, 8}, {8, 3}, {13, 8}, {8, 13}};
const byte clock_hands[][2] PROGMEM = {{4, 8}, {5, 8}, {6, 8}, {7, 8}, {8, 8}, {8, 9}, {8, 10}, {8, 11}};

void drawClock(){
	clearAll();

	CRGB colorBoundary = CRGB::Brown, colorHands = CRGB::LawnGreen, colorMarkings = CRGB::DarkGray, colorLoad = CRGB::Yellow;
	int i, size;

	//circle
	size = sizeof(clock_circle)/sizeof(clock_circle[0]);
	for(i = 0; i < size; ++i){
		int r = get_var(&(clock_circle[i][0])), c = get_var(&(clock_circle[i][1]));
		leds[handleCoordinates(r, c)] = colorBoundary;
	}
	

	//markings
	size = sizeof(clock_markings)/sizeof(clock_markings[0]);
	for(i = 0; i < size; ++i){
		int r = get_var(&(clock_markings[i][0])), c = get_var(&(clock_markings[i][1]));
		leds[handleCoordinates(r, c)] = colorMarkings;
	}

	//hands
	size = sizeof(clock_hands)/sizeof(clock_hands[0]);
	for(i = 0; i < size; ++i){
		int r = get_var(&(clock_hands[i][0])), c = get_var(&(clock_hands[i][1]));
		leds[handleCoordinates(r, c)] = colorHands;
	}

	FastLED.show();
	delay(1000);

	for(i = 1; i < ROWCOUNT - 1; i += 2){
		leds[handleCoordinates(i, COLUMNCOUNT)] = colorLoad;
		leds[handleCoordinates(i+1, COLUMNCOUNT)] = colorLoad;
		FastLED.show();
		if(switchUpdateCheck())
			return;
		delay(500);
	}
}

const byte app_android[][2] PROGMEM = {{3, 8}, {3, 9}, {4, 7}, {4, 8}, {4, 9}, {4, 10}, {5, 7}, {5, 8}, {5, 9}, {5, 10}, {6, 6}, 
	{6, 7}, {6, 8}, {6, 9}, {6, 10}, {6, 11}, {7, 7}, {7, 8}, {7, 9}, {7, 10}, {8, 7}, {8, 8}, {8, 9}, {8, 10}, {9, 7}, {9, 10}, 
	{10, 7}, {10, 10}};

void drawApp(){
	clearAll();

	CRGB colorBoundary = CRGB::White, colorAndroid = CRGB::LawnGreen, colorButton = CRGB::DarkGray, colorLoad = CRGB::Brown;
	int i;

	//boundary
	drawBox(2, 14, 5, 12, colorBoundary);

	//android
	int android_size = sizeof(app_android)/sizeof(app_android[0]);
	for(i = 0; i < android_size; ++i){
		int r = get_var(&(app_android[i][0])), c = get_var(&(app_android[i][1]));
		leds[handleCoordinates(r, c)] = colorAndroid;
	}

	//button
	drawBox(12, 13, 8, 9, colorButton);

	FastLED.show();
	delay(1000);

	for(i = 1; i < ROWCOUNT - 1; i += 2){
		leds[handleCoordinates(i, COLUMNCOUNT)] = colorLoad;
		leds[handleCoordinates(i+1, COLUMNCOUNT)] = colorLoad;
		FastLED.show();
		if(switchUpdateCheck())
			return;
		delay(500);
	}
}

//SAVE CHARACTERS TO PROGRAM MEMORY

const byte char_A[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 1, 1}, {1, 0, 1}, {1, 0, 1}};
const byte char_B[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 0}, {1, 0, 1}, {1, 1, 1}, {1, 0, 1}, {1, 1, 0}};
const byte char_C[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 1, 1}};
const byte char_D[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 0}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 1, 0}};
const byte char_E[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 0}, {1, 1, 1}, {1, 0, 0}, {1, 1, 1}};
const byte char_F[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 0}, {1, 1, 0}, {1, 0, 0}, {1, 0, 0}};
const byte char_G[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 0}, {1, 0, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_H[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 0, 1}, {1, 1, 1}, {1, 0, 1}, {1, 0, 1}};
const byte char_I[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {1, 1, 1}};
const byte char_J[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {0, 1, 0}, {0, 1, 0}, {1, 1, 0}, {0, 1, 0}};
const byte char_K[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 1, 0}, {1, 0, 0}, {1, 1, 0}, {1, 0, 1}};
const byte char_L[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 1, 1}};
const byte char_M[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 1, 1}, {1, 1, 1}, {1, 0, 1}, {1, 0, 1}};
const byte char_N[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}};
const byte char_O[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_P[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 1, 1}, {1, 0, 0}, {1, 0, 0}};
const byte char_Q[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}};
const byte char_R[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 1, 0}, {1, 0, 1}, {1, 0, 1}};
const byte char_S[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 0}, {1, 1, 1}, {0, 0, 1}, {1, 1, 1}};
const byte char_T[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}};
const byte char_U[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_V[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {0, 1, 0}};
const byte char_W[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 0, 1}, {1, 1, 1}, {1, 1, 1}, {1, 0, 1}};
const byte char_X[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 0, 1}, {0, 1, 0}, {1, 0, 1}, {1, 0, 1}};
const byte char_Y[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 0}, {0, 1, 0}};
const byte char_Z[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {0, 0, 1}, {1, 1, 1}, {1, 0, 0}, {1, 1, 1}};
const byte char_0[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_1[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 1, 0}, {1, 1, 0}, {0, 1, 0}, {0, 1, 0}, {1, 1, 1}};
const byte char_2[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {0, 0, 1}, {0, 1, 0}, {1, 0, 0}, {1, 1, 1}};
const byte char_3[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {0, 0, 1}, {1, 1, 1}, {0, 0, 1}, {1, 1, 1}};
const byte char_4[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 1}, {0, 1, 1}, {1, 0, 1}, {1, 1, 1}, {0, 0, 1}};
const byte char_5[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 0}, {1, 1, 0}, {0, 0, 1}, {1, 1, 0}};
const byte char_6[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 0}, {1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_7[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {0, 0, 1}, {0, 1, 0}, {1, 0, 0}, {1, 0, 0}};
const byte char_8[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_9[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 1, 1}, {1, 0, 1}, {1, 1, 1}, {0, 0, 1}, {1, 1, 1}};
const byte char_dot[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 1, 0}};
const byte char_colon[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 1, 0}, {0, 0, 0}, {0, 1, 0}, {0, 0, 0}};
const byte char_a[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {0, 1, 1}, {1, 0, 1}, {0, 1, 1}};
const byte char_b[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 0}, {1, 0, 0}, {1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_c[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 1, 1}, {1, 0, 0}, {1, 1, 1}};
const byte char_d[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 1}, {0, 0, 1}, {1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_e[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 1, 0}, {1, 1, 1}, {1, 0, 0}, {1, 1, 1}};
const byte char_f[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 1, 1}, {0, 1, 0}, {1, 1, 1}, {0, 1, 0}, {0, 1, 0}};
const byte char_g[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 1, 0}, {1, 1, 1}, {0, 0, 1}, {1, 1, 1}};
const byte char_h[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 0}, {1, 0, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 1}};
const byte char_i[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 1, 0}, {0, 0, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}};
const byte char_j[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 1, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 1, 0}};
const byte char_k[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{1, 0, 0}, {1, 0, 0}, {1, 0, 1}, {1, 1, 0}, {1, 0, 1}};
const byte char_l[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}};
const byte char_m[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 1, 1}, {1, 1, 1}, {1, 0, 1}};
const byte char_n[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 1}};
const byte char_o[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_p[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 0}, {1, 0, 0}};
const byte char_q[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {1, 1, 0}, {1, 1, 0}, {0, 1, 1}, {0, 1, 0}};
const byte char_r[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 1, 1}, {1, 0, 0}, {1, 0, 0}};
const byte char_s[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 1, 1}, {1, 1, 0}, {0, 1, 1}, {1, 1, 0}};
const byte char_t_[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 1, 0}, {0, 1, 0}, {1, 1, 1}, {0, 1, 0}, {0, 1, 1}};
const byte char_u[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 0, 1}, {1, 0, 1}, {1, 1, 1}};
const byte char_v[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 0, 1}, {1, 0, 1}, {0, 1, 0}};
const byte char_w[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 0, 1}, {1, 1, 1}, {1, 1, 1}};
const byte char_x[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 0, 1}, {0, 1, 0}, {1, 0, 1}};
const byte char_y[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {1, 0, 1}, {1, 0, 1}, {0, 1, 0}, {0, 1, 0}};
const byte char_z[CHAR_HEIGHT][CHAR_WIDTH] PROGMEM= {{0, 0, 0}, {0, 0, 0}, {1, 1, 1}, {0, 1, 0}, {1, 1, 1}};

byte* getCharacterImage(char c){
  switch(c){
    case 'A': return (byte*)char_A;
    case 'B': return (byte*)char_B;
    case 'C': return (byte*)char_C;
    case 'D': return (byte*)char_D;
    case 'E': return (byte*)char_E;
    case 'F': return (byte*)char_F;
    case 'G': return (byte*)char_G;
    case 'H': return (byte*)char_H;
    case 'I': return (byte*)char_I;
    case 'J': return (byte*)char_J;
    case 'K': return (byte*)char_K;
    case 'L': return (byte*)char_L;
    case 'M': return (byte*)char_M;
    case 'N': return (byte*)char_N;
    case 'O': return (byte*)char_O;
    case 'P': return (byte*)char_P;
    case 'Q': return (byte*)char_Q;
    case 'R': return (byte*)char_R;
    case 'S': return (byte*)char_S;
    case 'T': return (byte*)char_T;
    case 'U': return (byte*)char_U;
    case 'V': return (byte*)char_V;
    case 'W': return (byte*)char_W;
    case 'X': return (byte*)char_X;
    case 'Y': return (byte*)char_Y;
    case 'Z': return (byte*)char_Z;
	case 'a': return (byte*)char_a;
	case 'b': return (byte*)char_b;
	case 'c': return (byte*)char_c;
	case 'd': return (byte*)char_d;
	case 'e': return (byte*)char_e;
	case 'f': return (byte*)char_f;
	case 'g': return (byte*)char_g;
	case 'h': return (byte*)char_h;
	case 'i': return (byte*)char_i;
	case 'j': return (byte*)char_j;
	case 'k': return (byte*)char_k;
	case 'l': return (byte*)char_l;
	case 'm': return (byte*)char_m;
	case 'n': return (byte*)char_n;
	case 'o': return (byte*)char_o;
	case 'p': return (byte*)char_p;
	case 'q': return (byte*)char_q;
	case 'r': return (byte*)char_r;
	case 's': return (byte*)char_s;
	case 't': return (byte*)char_t_;
	case 'u': return (byte*)char_u;
	case 'v': return (byte*)char_v;
	case 'w': return (byte*)char_w;
	case 'x': return (byte*)char_x;
	case 'y': return (byte*)char_y;
	case 'z': return (byte*)char_z;
    case 0: 
    case '0': return (byte*)char_0;
    case 1:
    case '1': return (byte*)char_1;
    case 2:
	  case '2': return (byte*)char_2;
    case 3:
    case '3': return (byte*)char_3;
    case 4:
    case '4': return (byte*)char_4;
    case 5:
    case '5': return (byte*)char_5;
    case 6:
    case '6': return (byte*)char_6;
    case 7:
    case '7': return (byte*)char_7;
    case 8:
    case '8': return (byte*)char_8;
    case 9:
    case '9': return (byte*)char_9;
    default:
    case '.': return (byte*)char_dot;
    case ':': return (byte*)char_colon;
  }
}
