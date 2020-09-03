package com.steve.util;

import java.io.*;

public class ResourceFileReader
{
    public static String readResourceFile(String _classname, String _resourceFilename)
    {
        Class clazz = null;
        try {
            clazz = Class.forName(_classname);
        } catch (ClassNotFoundException ex)
        {
            ex.printStackTrace();
            return null;
        }
        InputStream fin = clazz.getResourceAsStream(_resourceFilename);
        return readFile(fin);
    }

    public static String readFile(String _path)
    {
        try
        {
            FileInputStream fin = new FileInputStream(_path);
            return readFile(fin);
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static String readFile(InputStream fin)
    {
        try {
            StringBuffer strBuf = new StringBuffer();
            int b, pos=0;
            boolean utf8bomflag=false;
            int []checkutf8bom = new int[3];
            while( (b = fin.read())!=-1)
            {
            	if(pos <3)
            	{
            		if(b==0xef && pos==0)
            		{
            			checkutf8bom[0]=b;
            			utf8bomflag = true;
            		}
            		else if(utf8bomflag && b==0xbb && pos==1)
            			checkutf8bom[1]=b;
            		else if(utf8bomflag && b==0xbf && pos==2)
            			checkutf8bom[2]=b;
            		else
            		{
            			strBuf.append((char)b);
            			utf8bomflag = false;
            		}
            		pos++;
            		continue;
            	}
                strBuf.append((char)b);
            } 
            return new String(strBuf);
        } catch (IOException ex1) {
            ex1.printStackTrace();
            return null;
        } finally {
            try {
                if(fin != null)
                {
                    fin.close();
                }
            } catch (IOException ex2) {
                ex2.printStackTrace();

            }
        }
    }
}
