package com.dmr;

public class EmbeddedLC {
	private boolean rawLC[]=new boolean[128];
	private int currentState=-1;
	private boolean dataReady=false;
	private String lines[]=new String[3];
	private boolean lcData[]=new boolean[72];
	
	// Add LC data (which may consist of 4 blocks) to the data store
	public void addData (byte[] dibit_buf,int type)	{
		int a,r=0;
		boolean rawdata[]=new boolean[32];
		// Convert from dibits into boolean
		for (a=70;a<86;a++)	{
			if (dibit_buf[a]==0)	{
				rawdata[r]=false;
				rawdata[r+1]=false;
			}
			else if (dibit_buf[a]==1)	{
				rawdata[r]=false;
				rawdata[r+1]=true;
			}
			else if (dibit_buf[a]==2)	{
				rawdata[r]=true;
				rawdata[r+1]=false;
			}
			else if (dibit_buf[a]==3)	{
				rawdata[r]=true;
				rawdata[r+1]=true;
			}
			r=r+2;
		}
		// Is this the first block of a 4 block embedded LC ?
		if (type==1)	{
			for (a=0;a<32;a++)	{
				rawLC[a]=rawdata[a];
			}
			// Show we are ready for the next LC block
			currentState=0;
		}
		// Is this the 2nd block of a 4 block embedded LC ?
		else if ((type==3)&&(currentState==0))	{
			for (a=0;a<32;a++)	{
				rawLC[a+32]=rawdata[a];
			}
			// Show we are ready for the next LC block
			currentState=1;
		}
		// Is this the 3rd block of a 4 block embedded LC ?
		else if ((type==3)&&(currentState==1))	{
			for (a=0;a<32;a++)	{
				rawLC[a+64]=rawdata[a];
			}
			// Show we are ready for the final LC block
			currentState=2;
		}
		// Is this the final block of a 4 block embedded LC ?
		else if ((type==2)&&(currentState==2))	{
			for (a=0;a<32;a++)	{
				rawLC[a+96]=rawdata[a];
			}
			// Process the complete data block
			processMultiBlockEmbeddedLC();
		}
		// Is this a single block embedded LC
		else if (type==0) processSingleBlockEmbeddedLC(rawdata);
	}
	
	// Unpack and error check a embedded LC
	private boolean processMultiBlockEmbeddedLC()	{
		int a,b=0,crc;
		boolean data[]=new boolean[128];
		boolean row[]=new boolean[16];
		lines[0]="Embedded Multi Block LC : ";
		// The data is unpacked downwards in columns
		for (a=0;a<128;a++)	{
			data[b]=rawLC[a];
			b=b+16;
			if (b>112) b=b-112;
		}
		// Hamming (16,11,4) check each row except the last one
		for (a=0;a<=96;a=a+16)	{
			for (b=0;b<16;b++)	{
				row[b]=data[a+b];
			}
			if (hamming16114(row)==false) return false;
		}
		// We have passed the Hamming check so extract the actual payload
		b=0;
		for (a=0;a<11;a++)	{
			lcData[b]=data[a];
			b++;
		}
		for (a=16;a<27;a++)	{
			lcData[b]=data[a];
			b++;
		}
		for (a=32;a<42;a++)	{
			lcData[b]=data[a];
			b++;
		}
		for (a=48;a<58;a++)	{
			lcData[b]=data[a];
			b++;
		}
		for (a=64;a<74;a++)	{
			lcData[b]=data[a];
			b++;
		}
		for (a=80;a<90;a++)	{
			lcData[b]=data[a];
			b++;
		}
		for (a=96;a<106;a++)	{
			lcData[b]=data[a];
			b++;
		}
		// Extract the 5 bit CRC
		if (data[42]==true) crc=16;
		else crc=0;
		if (data[58]==true) crc=crc+8;
		if (data[74]==true) crc=crc+4;
		if (data[90]==true) crc=crc+2;
		if (data[106]==true) crc++;
		// Now CRC check this
		crc tCRC=new crc();
		if (tCRC.crcFiveBit(lcData,crc)==false) return false;
		
		for (a=0;a<72;a++)	{
			if (lcData[a]==false) lines[0]=lines[0]+"0";
			else lines[0]=lines[0]+"1";
		}

		dataReady=true;
		return true;
	}
	
	// A Hamming (16,11,4) Check
	private boolean hamming16114 (boolean d[])	{
		boolean c[]=new boolean[5];
		// Calculate the checksum this column should have
		c[0]=d[0]^d[1]^d[2]^d[3]^d[5]^d[7]^d[8];
		c[1]=d[1]^d[2]^d[3]^d[4]^d[6]^d[8]^d[9];
		c[2]=d[2]^d[3]^d[4]^d[5]^d[7]^d[9]^d[10];
		c[3]=d[0]^d[1]^d[2]^d[4]^d[6]^d[7]^d[10];
		c[4]=d[0]^d[2]^d[5]^d[6]^d[8]^d[9]^d[10];
		// Compare these with the actual bits
		if ((c[0]==d[11])&&(c[1]==d[12])&&(c[2]==d[13])&&(c[3]==d[14])&&(c[4]==d[15])) return true;
		else return false;
	}
	
	// Deal with a single block embedded LC
	private void processSingleBlockEmbeddedLC (boolean data[])	{
		boolean isnull=true;
		String tline="";
		int a;
		lines[0]="Embedded Single Block LC : ";
		// Check if this message is all 0's as if it is then it is a null
		for (a=0;a<32;a++)	{
			if (data[a]==true)	{
				tline=tline+"1";
				isnull=false;
			}
			else 	{
				tline=tline+"0";
			}
		}
		// Is this message a null short LC
		if (isnull==true)	{
			lines[0]=lines[0]+"Null";
		}
		else	{
			lines[0]=lines[0]+tline;
		}
		dataReady=true;
	}
	
	// Tell the main program if we have data to return
	public boolean getDataReady	()	{
		return dataReady;
	}
	
	// Return the display lines to the main program
	public String[] getLines ()	{
		dataReady=false;
		currentState=-1;
		return lines;
	}

}