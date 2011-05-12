package de.hsrm.objectify.utils;

public class ImageHelper {
	
	public static int[] convertByteArray(byte[] byteArray) {
		int[] intArray = new int[((int) byteArray.length/4)];
		int idx = 0;
		for (int i=0; i<byteArray.length;i+=4) {
			intArray[idx] = (0xFF & byteArray[i]) << 24 |
							(0xFF & byteArray[i+1]) << 16 |
							(0xFF & byteArray[i+2]) << 8 |
							(0xFF & byteArray[i+3]) << 0;
			idx += 1;
		}
		return intArray;
	}

}