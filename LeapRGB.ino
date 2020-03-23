#include <WebSocketServer.h>
#include<FastLED.h>
#include<ESP8266WiFi.h>
#include <NTPClient.h>
#include <WiFiUdp.h>

//FILL ALL OF THESE VARIABLES ACCORDING TO SETUP
const char *MYSSID = "", *PASSWORD = "", *DELIMITER = "/";
const int DATAPIN = 5, SWITCHPIN = D2;
const int COLUMNCOUNT = 16, ROWCOUNT=15, LEDCOUNT = ROWCOUNT*COLUMNCOUNT;
const bool isConnectedAtTop = false, isConnectedAtLeft = true ;

const int LEDBRIGHTNESS = 10, PRINTDELAY = 150;

/*
 NOTES
 * PASS THE HEXCODE WITHOUT '#' I.E ONLY THE HEX VALUE
 * DO THE WIRING ROWWISE ONLY
 * Defined Messages = [setlight, setoff, show, clear, clearrow]
 * Format :- Call/Row/Column/Hex     (as needed)
*/

//USER-DEFINED FUNCTIONS
int handleCoordinates(int Row, int Column);
void clearall();
bool switchUpdateCheck();

int handleDigitPrint(int digit,const int PRINTROW,  int printColumn, CRGB color, bool shouldSpace);
void printToDisplay(const int number[][2], int count, const int PRINTROW, int printColumn, CRGB color, bool shouldSpace);
void shiftLeft();

void clockModeMain();
void drawClock();
void handleDayPrint(int day, const int PRINTROW, int printColumn, const CRGB colorScheme[2]);

void appModeMain();
void drawApp();
void printIP(IPAddress ip);
void setlight();
void setoff();
void clearrow();

void doAnimation1();
void doAnimation2();
void drawbox(int rowup, int rowdown, int colleft, int colright, CRGB color);

//GLOBAL VARIABLES 
const int MODE_APP = 0, MODE_CLOCK = 1;
int currentMode = MODE_CLOCK;
CRGB leds[LEDCOUNT];
WebSocketServer webSocketServer;

void setup() {
	Serial.begin(115200);
	FastLED.addLeds<WS2811, DATAPIN>(leds, LEDCOUNT);
	FastLED.setBrightness(LEDBRIGHTNESS); 
	
	pinMode(SWITCHPIN, INPUT_PULLUP);
	
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

	clearall();
}

void loop(){
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

void clockModeMain(){

	drawClock();
	if(currentMode != MODE_CLOCK){   //4 second window
		//button has been pressed
		return;
	}

	clearall();
    
	const int UTC_OFFSET_MS = 5.5 * 60 * 60;		//timezone [UTC + 5:30]
	const int TIMEROW = 2, DAYROW = 10, COLUMN_DAY = 1, COLUMN_DATE[2] = {10, 13}, COLUMN_HOURS[2] = {1, 4}, COLUMN_COLON = 8, COLUMN_MINUTES[2] = {10,13};
	const CRGB colorTime[] = {CRGB::Orange, CRGB::Blue}, colorTimeSub = CRGB::Chartreuse; 
	
	const int colon[][2] = {{1,0}, {3,0}};
	
	WiFiUDP ntpUDP;
	NTPClient timeClient(ntpUDP, "pool.ntp.org", UTC_OFFSET_MS);
	
	int timeDigits[4] = {-1, -1, -1, -1}, dayDigit = -1, dateDigits[2];
	String date = "";
	timeClient.begin();
	
	printToDisplay(colon, 2, TIMEROW, COLUMN_COLON, colorTimeSub, false);
	int i;
	for(i = 1;i <= COLUMNCOUNT; ++i)
		leds[handleCoordinates(DAYROW-2, i)] = colorTimeSub;
	for(i = DAYROW-1; i <= ROWCOUNT; ++i)
		leds[handleCoordinates(i, COLUMN_COLON)] = colorTimeSub;
	
	while(currentMode == MODE_CLOCK){
		timeClient.update();
		if(timeDigits[0] != timeClient.getHours() / 10){			
			timeDigits[0] = timeClient.getHours()/10;
			handleDigitPrint(timeDigits[0], TIMEROW, COLUMN_HOURS[0], colorTime[0], false);
		}
		
		if(timeDigits[1] != timeClient.getHours() % 10){	
			timeDigits[1] = timeClient.getHours()%10;
			handleDigitPrint(timeDigits[1], TIMEROW, COLUMN_HOURS[1], colorTime[1], false);
		}
	
		if(timeDigits[2] != timeClient.getMinutes() / 10){
			timeDigits[2] = timeClient.getMinutes()/10;
			handleDigitPrint(timeDigits[2], TIMEROW, COLUMN_MINUTES[0], colorTime[0], false);
		}
		
		if(timeDigits[3] != timeClient.getMinutes() % 10){		
			timeDigits[3] = timeClient.getMinutes()%10;
			handleDigitPrint(timeDigits[3], TIMEROW, COLUMN_MINUTES[1], colorTime[1], false);
		}
		
		if(dayDigit != timeClient.getDay()){
			dayDigit = timeClient.getDay();
			handleDayPrint(dayDigit, DAYROW, COLUMN_DAY, colorTime);
		}
		
		//YYYY-MM-DDT.....
		if(!date.equals(timeClient.getFormattedDate().substring(8, 10))){
			date = timeClient.getFormattedDate().substring(8, 10);
			int dateInt = date.toInt();
			dateDigits[0] = dateInt/10;
			dateDigits[1] = dateInt%10;
			handleDigitPrint(dateDigits[0], DAYROW, COLUMN_DATE[0], colorTime[0], false);
			handleDigitPrint(dateDigits[1], DAYROW, COLUMN_DATE[1], colorTime[1], false);
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

	clearall();
  
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
			char dataArray[30];
		 
			while(client.connected()){
				//Receive Data From Client
				dataString = webSocketServer.getData(); 
		  
				if (dataString.length() > 0) {
					//Handle the Received Data
					dataString.toCharArray(dataArray, 30);

					char* toCall = strtok(dataArray, DELIMITER);
					
					if(strcmp(toCall, "setlight") == 0)
						setlight();
					else if(strcmp(toCall, "setoff") == 0)
						setoff();
					else if(strcmp(toCall, "show") == 0)
						FastLED.show();
					else if(strcmp(toCall, "clear") == 0)
						clearall();
					else if(strcmp(toCall, "clearrow") ==0)
						clearrow();
					else if(strcmp(toCall, "animation1") ==0)
						doAnimation1();
					else if(strcmp(toCall, "animation2") ==0)
						doAnimation2();
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
			Serial.println("Done Printing IP");
			delay(PRINTDELAY);
			clearall();
			notConnectedIterations = 0;
		}   
		delay(100);
	}
  server.close();
}

//Receives the coordinate and color to light up an LED
void setlight() {
	int Row, Column, Hex;
	char *dataValue;
  
	dataValue = strtok(NULL, DELIMITER);
	Row = atoi(dataValue);
  
	dataValue = strtok(NULL, DELIMITER);
	Column = atoi(dataValue);
  
	dataValue = strtok(NULL, DELIMITER);
	Hex = strtol(dataValue, NULL, 16);  //base 16 conversion
  
	int ledno = handleCoordinates(Row, Column);
	Serial.println(String(Row) + " " + String(Column) +" "+ String(Hex)+" LED No"+ledno);
	leds[ledno] = Hex;
}

//Sets the LED at some coordinate to off
void setoff() {
	int Row, Column;
	char *dataValue;
  
	dataValue = strtok(NULL, DELIMITER);
	Row = atoi(dataValue);
  
	dataValue = strtok(NULL, DELIMITER);
	Column = atoi(dataValue);
  
	int ledno = handleCoordinates(Row, Column);
	Serial.println("Turned off"+String(Row)+" "+String(Column));
	leds[ledno] = CRGB::Black;
}

//Handles the Row and Column according to the wiring. Needed because wiring is L->R R->L ....
int handleCoordinates(int Row, int Column){
	int ledno;
 
	if(isConnectedAtTop && isConnectedAtLeft){  //TL
		if(Row%2)   //Odd Row i.e L->R row
			ledno = (Row - 1) * COLUMNCOUNT + Column - 1;
		else        //Even Row i.e R->L row
			ledno = Row * COLUMNCOUNT - Column;
	}

	else if(isConnectedAtTop && !isConnectedAtLeft){  //TR
		if(Row%2)   //Odd Row i.e L->R row
			ledno = Row * COLUMNCOUNT - Column;
		else        //Even Row i.e R->L row
			ledno = (Row - 1) * COLUMNCOUNT + Column - 1;
    }

	else if(!isConnectedAtTop && !isConnectedAtLeft){ //BR
		if(Row%2)   //Odd Row i.e R->L row
			ledno = (ROWCOUNT - Row) * COLUMNCOUNT + (COLUMNCOUNT - Column);
		else        //Even Row i.e L->R row
			ledno = (ROWCOUNT - Row) * COLUMNCOUNT + Column - 1;
    }

	else{ //BL
		if(Row%2)   //Odd Row i.e L->R row
			ledno = (ROWCOUNT - Row) * COLUMNCOUNT + Column - 1;
		else        //Even Row i.e R->L row
			ledno = (ROWCOUNT - Row) * COLUMNCOUNT + (COLUMNCOUNT - Column);  
    }
	
	return ledno;    
}

//Turns off all the LEDs
void clearall(){
	for(int i = 0; i < LEDCOUNT; ++i)
		leds[i] = CRGB::Black;
	FastLED.show();
	Serial.println("All cleared");
}

void clearrow(){
	int Row, ColumnStart, ColumnEnd;
	char *dataValue;
  
	dataValue = strtok(NULL, DELIMITER);
	Row = atoi(dataValue);
	dataValue = strtok(NULL, DELIMITER);
	ColumnStart = atoi(dataValue);
	dataValue = strtok(NULL, DELIMITER);
	ColumnEnd = atoi(dataValue);

	for(int i = ColumnStart; i <= ColumnEnd; ++i)
		leds[handleCoordinates(Row, i)] = CRGB::Black;
	FastLED.show();
}

void drawbox(int rowup, int rowdown, int colleft, int colright, CRGB color){
	int i;
	for(i = colleft; i <= colright; ++i){
		leds[handleCoordinates(rowup,i)] = color;
		leds[handleCoordinates(rowdown,i)] = color;
    }
	
	for(i = rowup+1; i < rowdown; ++i){
		leds[handleCoordinates(i,colleft)] = color;
		leds[handleCoordinates(i,colright)] = color;
    }   
}

void printIP(IPAddress ip){
	const int IP_PRINTROW = 6;
	const CRGB colorIP = CRGB::White;
	
	const int dot[][2] = {{4,0}};

	int currentColumn = 1;

	clearall();
  
	int i;
	for(i = 0; i < 4; ++i){ //ip is 4 integers
		int ipno =ip[i];
		int temp = 0;
		
		//reverse the number before passing to get digits
		do{
			temp = temp*10 + ipno%10;
			ipno = ipno/10;
		}while(ipno);

		ipno = temp;
    
		while(ipno){
			int  digit = ipno%10;
			currentColumn = handleDigitPrint(digit, IP_PRINTROW, currentColumn, colorIP, true);  //returns updated currentColumn within the function
			ipno = ipno / 10;
		}
		
		if(i<3){  //no dot at the end
			printToDisplay(dot, 1, IP_PRINTROW, currentColumn, colorIP, true);
			currentColumn += 2;
		}
	} 
}

int handleDigitPrint(int digit,const int PRINTROW, int printColumn, CRGB color, bool shouldSpace){
	//(row,column form)
	const static int number0[][2] = {{0,0},{0,1},{0,2},{1,0},{1,2},{2,0},{2,2},{3,0},{3,2},{4,0},{4,1},{4,2}};
	const static int number1[][2] = {{0,1},{1,0},{1,1},{2,1},{3,1},{4,0},{4,1},{4,2}};
	const static int number2[][2] = {{0,0},{0,1},{0,2},{1,2},{2,1},{3,0},{4,0},{4,1},{4,2}};
	const static int number3[][2] = {{0,0},{0,1},{0,2},{1,2},{2,0},{2,1},{2,2},{3,2},{4,0},{4,1},{4,2}};
	const static int number4[][2] = {{0,2},{1,1},{1,2},{2,0},{2,2},{3,0},{3,1},{3,2},{4,2}};
	const static int number5[][2] = {{0,0},{0,1},{0,2},{1,0},{2,0},{2,1},{3,2},{4,0},{4,1}};
	const static int number6[][2] = {{0,0},{0,1},{0,2},{1,0},{2,0},{2,1},{2,2},{3,0},{3,2},{4,0},{4,1},{4,2}};
	const static int number7[][2] = {{0,0},{0,1},{0,2},{1,2},{2,1},{3,0},{4,0}};
	const static int number8[][2] = {{0,0},{0,1},{0,2},{1,0},{1,2},{2,0},{2,1},{2,2},{3,0},{3,2},{4,0},{4,1},{4,2}};
	const static int number9[][2] = {{0,0},{0,1},{0,2},{1,0},{1,2},{2,0},{2,1},{2,2},{3,2},{4,0},{4,1},{4,2}};
 
	const int NUMBERWIDTH = 3;
	
	switch(digit){
		case 0: 
			printToDisplay(number0, sizeof(number0)/sizeof(number0[0]), PRINTROW, printColumn, color, true);
			break;
		case 1: 
			printToDisplay(number1, sizeof(number1)/sizeof(number1[0]), PRINTROW, printColumn, color, true);
			break;
		case 2: 
			printToDisplay(number2, sizeof(number2)/sizeof(number2[0]), PRINTROW, printColumn, color, true);
			break;
		case 3: 
			printToDisplay(number3, sizeof(number3)/sizeof(number3[0]), PRINTROW, printColumn, color, true);
			break;
		case 4: 
			printToDisplay(number4, sizeof(number4)/sizeof(number4[0]), PRINTROW, printColumn, color, true);
			break;
		case 5: 
			printToDisplay(number5, sizeof(number5)/sizeof(number5[0]), PRINTROW, printColumn, color, true);
			break;
		case 6: 
			printToDisplay(number6, sizeof(number6)/sizeof(number6[0]), PRINTROW, printColumn, color, true);
			break;
		case 7: 
			printToDisplay(number7, sizeof(number7)/sizeof(number7[0]), PRINTROW, printColumn, color, true);
			break;
		case 8: 
			printToDisplay(number8, sizeof(number8)/sizeof(number8[0]), PRINTROW, printColumn, color, true);
			break;
		case 9: 
			printToDisplay(number9, sizeof(number9)/sizeof(number9[0]), PRINTROW, printColumn, color, true);
			break;
	}
	
	return (printColumn + NUMBERWIDTH + (shouldSpace?1:0));
}

void handleDayPrint(int day, const int PRINTROW, int printColumn, const CRGB colorScheme[2]){
	const static int sunsatS[][2] = {{0,1},{0,2},{1,0},{2,0},{2,1},{2,2},{3,2},{4,0},{4,1}};
	const static int suntueu[][2] = {{2,0},{2,2},{3,0},{3,2},{4,0},{4,1},{4,2}};
	const static int mondayM[][2] = {{0,0},{0,2},{1,0},{1,1},{1,2},{2,0},{2,1},{2,2},{3,0},{3,2},{4,0},{4,2}};
	const static int mondayo[][2] = {{2,0},{2,1},{2,2},{3,0},{3,2},{4,0},{4,1},{4,2}};
	const static int tuethursT[][2] = {{0,0},{0,1},{0,2},{1,1},{2,1},{3,1},{4,1}};
	const static int wednesdayW[][2] = {{0,0},{0,2},{1,0},{1,2},{2,0},{2,2},{3,0},{3,1},{3,2},{4,0},{4,2}};
	const static int wednesdayd[][2] = {{0,2},{1,2},{2,0},{2,1},{2,2},{3,0},{3,2},{4,0},{4,1},{4,2}};
	const static int thursdayh[][2] = {{0,0},{1,0},{2,0},{2,1},{2,2},{3,0},{3,2},{4,0},{4,2}};
	const static int fridayF[][2] = {{0,0},{0,1},{0,2},{1,0},{2,0},{2,1},{2,2},{3,0},{4,0}}; 
	const static int fridayr[][2] = {{2,0},{2,1},{2,2},{3,0},{4,0}}; 	
	const static int saturdaya[][2] = {{2,1},{2,2},{3,0},{3,2},{4,1},{4,2}};
	
	switch(day){
		case 0:	//sunday
			printToDisplay(sunsatS, sizeof(sunsatS)/sizeof(sunsatS[0]), PRINTROW, printColumn, colorScheme[0], false);
			printToDisplay(suntueu, sizeof(suntueu)/sizeof(suntueu[0]), PRINTROW, printColumn+3, colorScheme[1], false);
			break;
		case 1:	//monday 
			printToDisplay(mondayM, sizeof(mondayM)/sizeof(mondayM[0]), PRINTROW, printColumn, colorScheme[0], false);
			printToDisplay(mondayo, sizeof(mondayo)/sizeof(mondayo[0]), PRINTROW, printColumn+3, colorScheme[1], false);
			break;
		case 2:	//tuesday 
			printToDisplay(tuethursT, sizeof(tuethursT)/sizeof(tuethursT[0]), PRINTROW, printColumn, colorScheme[0], false);
			printToDisplay(suntueu, sizeof(suntueu)/sizeof(suntueu[0]), PRINTROW, printColumn+3, colorScheme[1], false);
			break;
		case 3:	//wednesday 
			printToDisplay(wednesdayW, sizeof(wednesdayW)/sizeof(wednesdayW[0]), PRINTROW, printColumn, colorScheme[0], false);
			printToDisplay(wednesdayd, sizeof(wednesdayd)/sizeof(wednesdayd[0]), PRINTROW, printColumn+3, colorScheme[1], false);
			break;
		case 4:	//thursday 
			printToDisplay(tuethursT, sizeof(tuethursT)/sizeof(tuethursT[0]), PRINTROW, printColumn, colorScheme[0], false);
			printToDisplay(thursdayh, sizeof(thursdayh)/sizeof(thursdayh[0]), PRINTROW, printColumn+3, colorScheme[1], false);
			break;
		case 5:	//friday
			printToDisplay(fridayF, sizeof(fridayF)/sizeof(fridayF[0]), PRINTROW, printColumn, colorScheme[0], false);
			printToDisplay(fridayr, sizeof(fridayr)/sizeof(fridayr[0]), PRINTROW, printColumn+3, colorScheme[1], false);
			break;
		case 6:	//saturday 
			printToDisplay(sunsatS, sizeof(sunsatS)/sizeof(sunsatS[0]), PRINTROW, printColumn, colorScheme[0], false);
			printToDisplay(saturdaya, sizeof(saturdaya)/sizeof(saturdaya[0]), PRINTROW, printColumn+3, colorScheme[1], false);
			break;
	}
}

void printToDisplay(const int number[][2], int count, const int PRINTROW, int printColumn, CRGB color, bool shouldSpace){
  
	int printWidth = (count > 5)? 3 : 1;  // symbol or number?   [assuming symbols only occupy one line ie 5 pixels]

	if(shouldSpace)
		++printWidth;
		
	int relativeRow, relativeColumn;
	int i, j;
	for(i = 0; i < printWidth; ++i){
    
		if(printColumn > COLUMNCOUNT){
			shiftLeft();
			printColumn = 16;
		}
   
		//clear column before printing. 5 is height.
		for(j = 0; j < 5; ++j)
			leds[handleCoordinates(PRINTROW + j, printColumn)] = CRGB::Black; 
    
		for(j = 0; j < count; ++j){
			relativeRow = number[j][0];
			relativeColumn = number[j][1];
			if(relativeColumn == i){
				Serial.println("Printed at" + String(PRINTROW + relativeRow)+ ", "+ String(printColumn));
				leds[handleCoordinates(PRINTROW + relativeRow, printColumn)] = color;
			}    
		}
    
		FastLED.show();
		delay(PRINTDELAY);
		++printColumn;;
	}
}

void shiftLeft(){
	int i, j;
	for(i = 1; i < COLUMNCOUNT; ++i)
		for(j = 1; j <= ROWCOUNT; ++j)
			leds[handleCoordinates(j, i)] = leds[handleCoordinates(j, i + 1)];

	for(j = 1; j <= ROWCOUNT; ++j)
		leds[handleCoordinates(j, COLUMNCOUNT)] = CRGB::Black;
      
	Serial.println("Matrix leftshifted");    
}  
  
void doAnimation1(){
	CRGB colors[8]={CRGB::White, CRGB::Violet, CRGB::Indigo, CRGB::Blue, CRGB::Green, CRGB::Yellow, CRGB::Orange, CRGB::Red};
	int rowup=8,rowdown=8,colleft=8,colright=9;
	int i;
	String dataString;
	while(true){
		dataString = webSocketServer.getData();
			if(dataString.length() > 0) //if any thing is received
				break;
		for(i=0;i<=7;++i){
			drawbox(rowup-i,rowdown+i,colleft-i,colright+i,colors[i]);
			FastLED.show();
			delay(50);
    
			drawbox(rowup-i,rowdown+i,colleft-i,colright+i,CRGB::Black);
		}
    
		for(i=6;i>=0;--i){
			drawbox(rowup - i,rowdown + i,colleft - i,colright + i,colors[i]);
			FastLED.show();
			delay(50);
    
			drawbox(rowup-i,rowdown+i,colleft-i,colright+i,CRGB::Black);
		}
    }
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
			drawbox(rowup-i,rowdown+i,colleft-i,colright+i,colors[i]);
			FastLED.show();
			delay(50);
		}
		
		delay(100);
		
		for(i=7; i>=0;--i){
			drawbox(rowup-i,rowdown+i,colleft-i,colright+i,CRGB::Black);
			FastLED.show();
			delay(50);
		}
    }
}

void drawClock(){
	clearall();

	CRGB colorBoundary = CRGB::Brown, colorHands = CRGB::LawnGreen, colorMarkings = CRGB::DarkGray, colorLoad = CRGB::Yellow;

	//circle
	leds[handleCoordinates(2,5)] = colorBoundary;
	leds[handleCoordinates(2,6)] = colorBoundary;
	leds[handleCoordinates(2,7)] = colorBoundary;
	leds[handleCoordinates(2,8)] = colorBoundary;
	leds[handleCoordinates(2,9)] = colorBoundary;
	leds[handleCoordinates(2,10)] = colorBoundary;
	leds[handleCoordinates(2,11)] = colorBoundary;
	leds[handleCoordinates(3,4)] = colorBoundary;
	leds[handleCoordinates(3,5)] = colorBoundary;
	leds[handleCoordinates(3,11)] = colorBoundary;
	leds[handleCoordinates(3,12)] = colorBoundary;
	leds[handleCoordinates(4,3)] = colorBoundary;
	leds[handleCoordinates(4,4)] = colorBoundary;
	leds[handleCoordinates(4,12)] = colorBoundary;
	leds[handleCoordinates(4,13)] = colorBoundary;
	leds[handleCoordinates(5,2)] = colorBoundary;
	leds[handleCoordinates(5,3)] = colorBoundary;
	leds[handleCoordinates(5,13)] = colorBoundary;
	leds[handleCoordinates(5,14)] = colorBoundary;
	leds[handleCoordinates(6,2)] = colorBoundary;
	leds[handleCoordinates(6,14)] = colorBoundary;
	leds[handleCoordinates(7,2)] = colorBoundary;
	leds[handleCoordinates(7,14)] = colorBoundary;
	leds[handleCoordinates(8,2)] = colorBoundary;
	leds[handleCoordinates(8,14)] = colorBoundary;
	leds[handleCoordinates(9,2)] = colorBoundary;
	leds[handleCoordinates(9,14)] = colorBoundary;
	leds[handleCoordinates(10,2)] = colorBoundary;
	leds[handleCoordinates(10,14)] = colorBoundary;
	leds[handleCoordinates(11,2)] = colorBoundary;
	leds[handleCoordinates(11,3)] = colorBoundary;
	leds[handleCoordinates(11,13)] = colorBoundary;
	leds[handleCoordinates(11,14)] = colorBoundary;
	leds[handleCoordinates(12,3)] = colorBoundary;
	leds[handleCoordinates(12,4)] = colorBoundary;
	leds[handleCoordinates(12,12)] = colorBoundary;
	leds[handleCoordinates(12,13)] = colorBoundary;
	leds[handleCoordinates(13,4)] = colorBoundary;
	leds[handleCoordinates(13,5)] = colorBoundary;
	leds[handleCoordinates(13,11)] = colorBoundary;
	leds[handleCoordinates(13,12)] = colorBoundary;
	leds[handleCoordinates(14,5)] = colorBoundary;
	leds[handleCoordinates(14,6)] = colorBoundary;
	leds[handleCoordinates(14,7)] = colorBoundary;
	leds[handleCoordinates(14,8)] = colorBoundary;
	leds[handleCoordinates(14,9)] = colorBoundary;
	leds[handleCoordinates(14,10)] = colorBoundary;
	leds[handleCoordinates(14,11)] = colorBoundary;


	//markings
	leds[handleCoordinates(3,8)] = colorMarkings;
	leds[handleCoordinates(8,3)] = colorMarkings;
	leds[handleCoordinates(13,8)] = colorMarkings;
	leds[handleCoordinates(8,13)] = colorMarkings;

	//hands

	leds[handleCoordinates(4,8)] = colorHands;
	leds[handleCoordinates(5,8)] = colorHands;
	leds[handleCoordinates(6,8)] = colorHands;
	leds[handleCoordinates(7,8)] = colorHands;
	leds[handleCoordinates(8,8)] = colorHands;
	leds[handleCoordinates(8,9)] = colorHands;
	leds[handleCoordinates(8,10)] = colorHands;
	leds[handleCoordinates(8,11)] = colorHands;

	FastLED.show();
	delay(1000);

	for(int i = 1; i < ROWCOUNT - 1; i += 2){
		leds[handleCoordinates(i, COLUMNCOUNT)] = colorLoad;
		leds[handleCoordinates(i+1, COLUMNCOUNT)] = colorLoad;
		FastLED.show();
		if(switchUpdateCheck())
			return;
		delay(500);
	}
}

void drawApp(){
	clearall();

	CRGB colorBoundary = CRGB::White, colorAndroid = CRGB::LawnGreen, colorButton = CRGB::DarkGray, colorLoad = CRGB::Brown;

	//boundary
	drawbox(2, 14, 5, 12, colorBoundary);

	//android
	leds[handleCoordinates(3,8)] = colorAndroid;
	leds[handleCoordinates(3,9)] = colorAndroid;
	leds[handleCoordinates(4,7)] = colorAndroid;
	leds[handleCoordinates(4,8)] = colorAndroid;
	leds[handleCoordinates(4,9)] = colorAndroid;
	leds[handleCoordinates(4,10)] = colorAndroid;
	leds[handleCoordinates(5,7)] = colorAndroid;
	leds[handleCoordinates(5,8)] = colorAndroid;
	leds[handleCoordinates(5,9)] = colorAndroid;
	leds[handleCoordinates(5,10)] = colorAndroid;
	leds[handleCoordinates(6,6)] = colorAndroid;
	leds[handleCoordinates(6,7)] = colorAndroid;
	leds[handleCoordinates(6,8)] = colorAndroid;
	leds[handleCoordinates(6,9)] = colorAndroid;
	leds[handleCoordinates(6,10)] = colorAndroid;
	leds[handleCoordinates(6,11)] = colorAndroid;
	leds[handleCoordinates(7,7)] = colorAndroid;
	leds[handleCoordinates(7,8)] = colorAndroid;
	leds[handleCoordinates(7,9)] = colorAndroid;
	leds[handleCoordinates(7,10)] = colorAndroid;
	leds[handleCoordinates(8,7)] = colorAndroid;
	leds[handleCoordinates(8,8)] = colorAndroid;
	leds[handleCoordinates(8,9)] = colorAndroid;
	leds[handleCoordinates(8,10)] = colorAndroid;
	leds[handleCoordinates(9,7)] = colorAndroid;
	leds[handleCoordinates(9,10)] = colorAndroid;
	leds[handleCoordinates(10,7)] = colorAndroid;
	leds[handleCoordinates(10,10)] = colorAndroid;

	//button
	drawbox(12, 13, 8, 9, colorButton);

	FastLED.show();
	delay(1000);

	for(int i = 1; i < ROWCOUNT - 1; i += 2){
		leds[handleCoordinates(i, COLUMNCOUNT)] = colorLoad;
		leds[handleCoordinates(i+1, COLUMNCOUNT)] = colorLoad;
		FastLED.show();
		if(switchUpdateCheck())
			return;
		delay(500);
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
