
#ifndef __FSWORD_T_TYPE
	#define __FSWORD_T_TYPE int
#endif
#ifndef __SYSCALL_SLONG_TYPE
	#define __SYSCALL_SLONG_TYPE long int
#endif
#ifndef __SYSCALL_ULONG_TYPE
	#define __SYSCALL_ULONG_TYPE unsigned long int
#endif

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "audio.h"

#define CONFIG_LEN 	    640//128 //refers to main.h



void AudioSDPAccept_AMR(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_AMR_track);
void AudioFileHeaderWrite_AMR(FILE *audio_file, const char *audio_config, const int audio_config_len);
void AudioFrameWrite_AMR(FILE *fp_aac,char *pkt, int len,const char *audio_config);

void AudioSDPAccept_AAC(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_AAC_track);
void AudioFileHeaderWrite_AAC(FILE *audio_file, const char *audio_config, const int audio_config_len);
void AudioFrameWrite_AAC(FILE *fp_aac,char *pkt, int len,const char *audio_config);

void AudioSDPAccept_G711(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_G711_track);
void AudioFileHeaderWrite_G711(FILE *audio_file, const char *audio_config, const int audio_config_len);
void AudioFrameWrite_G711(FILE *fp_aac,char *pkt, int len,const char *audio_config);
///\addtogroup avapi
///@{
	
///\brief 處理Audio Codec
///
///Add a new audio codec
///1. implement a new codec AudioHandling structure
///2. Add the new structure to handlers definition
///3. define a new macro that maps name and id for the codec in audio header
///4. Modify the MAX_AUDIO_CODEC to the max id
struct AudioHandling
{
	///SDP中codec的名字
	char codename[10];
	///nc中存檔檔案的副檔名
	char fileextname[10];
	///\brief 解譯SDP audio sdp_track
	///
	///取得audio_config，供AudioFileHeaderWrite和AudioFrameWrite兩個功能使用<br>
	///依據SDP內容，實作需指定fileExt
	///\param fileExt output，依據sdp內容，指定audio的格式
	///\param audio_config output，依據sdp內容，取得audio_config，供AudioFileHeaderWrite和AudioFrameWrite兩個功能使用
	///\param audio_config_len output，audio_config的長度(?bytes)
	///\param sdp_track input sdp的audio track字串
	void (*AudioSDPAccept)(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_track);
	///\brief 寫入audio檔案的檔頭
	///
	///寫入audio檔案的檔頭
	///\param audio_file input，欲寫入檔案的FILE結構指標
	///\param audio_config input，AudioSDPAccept回傳的audio_config
	///\param audio_config_len input，AudioSDPAccept回傳的audio_config_len
	void (*AudioFileHeaderWrite)(FILE *audio_file, const char *audio_config, const int audio_config_len);
	///\brief 寫入audio串流到檔案
	///
	///寫入audio串流到檔案
	///\param fp_aac input，欲寫入檔案的FILE結構指標
	///\param pkt input，指向串流的指標
	///\param len input，串流的長度	
	///\param audio_config input，AudioSDPAccept回傳的audio_config
	void (*AudioFrameWrite)(FILE *fp_aac,char *pkt, int len,const char *audio_config);
};
///AudioHandling實作陣列
struct AudioHandling handlers[]=
{
	{"AMR",  "amr", AudioSDPAccept_AMR,   AudioFileHeaderWrite_AMR,  AudioFrameWrite_AMR },
	{"PCMU", "g711",AudioSDPAccept_G711, AudioFileHeaderWrite_G711, AudioFrameWrite_G711},
	{"AAC",  "aac", AudioSDPAccept_AAC,   AudioFileHeaderWrite_AAC,  AudioFrameWrite_AAC }
	
};
///@}
int AssertAudioCodecType(const int fileext)
{
	if(fileext<0 || fileext>MAX_AUDIO_CODEC)
		return AUDIO_NOK;
	return AUDIO_OK;
}

void AudioSDPAcceptHandling(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_audio_track)
{
	char *sdp_codec_track;
	int i;
	for(i=0;i<sizeof(handlers)/sizeof(struct AudioHandling);i++)
	{
		sdp_codec_track = strstr(sdp_audio_track, handlers[i].codename);
		if(sdp_codec_track !=NULL)
		{
			handlers[i].AudioSDPAccept(fileExt, audio_config, audio_config_len, sdp_codec_track);
			break;
		}
	}
}
void AudioFileHeaderWriteHandling(FILE *audio_file, const char *audio_config, const int audio_config_len, const int fileext)
{
	if(AssertAudioCodecType(fileext)==AUDIO_OK)
		handlers[fileext].AudioFileHeaderWrite(audio_file, audio_config, audio_config_len);
	else
		printf("Wrong AudioCodecType:%d",fileext);
}
void AudioFrameWriteHandling(FILE *fp_aac,char *pkt, int len,const char *audio_config, const int fileext)
{
	if(AssertAudioCodecType(fileext)==AUDIO_OK)
	{
		handlers[fileext].AudioFrameWrite(fp_aac, pkt, len, audio_config);
	}
	else
		printf("Wrong AudioCodecType:%d",fileext);
}

char* AudioFileExtHandling(int fileext)
{
	if(AssertAudioCodecType(fileext)==AUDIO_OK)
	{
		return &(handlers[fileext].fileextname[0]);
	}
	else
		printf("Wrong AudioCodecType:%d",fileext);
	return NULL;
}

int acc_rtp_receive(FILE *fp_aac,char *pkt, int len,const char *audio_config)
{//startcode+temp=ADTS AAC header   audio_config: sdp -> config=?  the value
    char config_decode[2],b2[4];
    char start_code[4] = {0xFF, 0xF1};
    char temp[3] = {0x00,0x00,0x00};
    int i,frame_number,frame_length,total_length;
    int byteWrite;


    b2[0]=((audio_config[0]&0x18)>>3)-1;
    b2[0]=b2[0]<<6;
    b2[1]=(audio_config[0]&0x07)<<3;
    b2[2]=(audio_config[1]&0x80)>>5;
    b2[3]=(audio_config[1]&0x20)>>5;
    config_decode[0]=b2[0]|b2[1]|b2[2]|b2[3];
    config_decode[1]=(audio_config[1]&0x18)<<3;

    frame_number = (int)((0x7F & (pkt[13] >>1)) >> 3);


    total_length = 0;
    for( i = 0 ; i < frame_number ; i++)
    {
        frame_length = 0;
        frame_length = (int)(pkt[12+2+2*i] << 5);
        frame_length = frame_length + (int)((pkt[12 + 2 + 2 * i + 1] & 0xF8) >> 3);

        start_code[2] = config_decode[0];
        start_code[3] = (config_decode[1] | (((frame_length + 7) & 0x1800) >> 11));
        if( (byteWrite=fwrite(start_code, 1, 4, fp_aac)) != 4)
        {
            printf("sec1. file write failed %d\n",byteWrite);
            exit(1);
        }
        temp[0] = (char)(0xFF & ((frame_length + 7) >> 3));
        temp[1] = (char)(0x07 & (frame_length+7));
        temp[1] = temp[1] << 5;
        temp[1] = (temp[1] | 0x1F);
        temp[2] = (char)0xFC;

        if( (byteWrite=fwrite(temp, 1, 3, fp_aac)) != 3)
        {
            printf("sec2. file write failed %d\n",byteWrite);
            exit(1);
        }

        if( (byteWrite=fwrite(&pkt[12 + 2 + frame_number * 2 + total_length], 1, frame_length, fp_aac)) != frame_length)
        {
            printf("sec3. file write failed %d %d\n",byteWrite,frame_length);
            exit(1);
        }
        total_length += frame_length;
    }

    return total_length;
}


void AudioSDPAccept_AAC(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_AAC_track)
{
	char *r, ch, ch2, val;
	int i;
	
	*fileExt=AAC;
	r = strstr(sdp_AAC_track, "config=");
	if(r!=NULL)
	{
		r+=7;
		for (i=0; *r && *r!=';' && *r!='\n' && *r!='\r' && i<CONFIG_LEN; r+=2)
		{
			ch=*r;
			ch2=*(r+1);

			if (ch >= '0' && ch <='9')
				ch=ch-'0';
			else if (ch >= 'A' && ch <= 'F')
				ch=ch-'A'+10;
			else if (ch >= 'a' && ch <= 'f')
				ch=ch-'a'+10;

			if (ch2 >= '0' && ch2 <='9')
				ch2=ch2-'0';
			else if (ch2 >= 'A' && ch2 <= 'F')
				ch2=ch2-'A'+10;
			else if (ch2 >= 'a' && ch2 <= 'f')
				ch2=ch2-'a'+10;

			val=(ch * 0x10 + ch2)&0xff;
			audio_config[i++]= val;
		}
		*audio_config_len=i;
	}
}
void AudioFileHeaderWrite_AAC(FILE *audio_file, const char *audio_config, const int audio_config_len)
{
	//do nothing
}
void AudioFrameWrite_AAC(FILE *fp_aac,char *pkt, int len,const char *audio_config)
{
	acc_rtp_receive(fp_aac, pkt, len, audio_config);
}

void AudioSDPAccept_AMR(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_AMR_track)
{
	*fileExt=AMR;
    //#!AMR\n (6 bytes)
    audio_config[0]='#';
    audio_config[1]='!';
    audio_config[2]='A';
    audio_config[3]='M';
    audio_config[4]='R';
    audio_config[5]='\n';
    *audio_config_len=6;
}

void AudioFileHeaderWrite_AMR(FILE *audio_file, const char *audio_config, const int audio_config_len)
{
	int i;
    for(i=0; i<audio_config_len; i++)
        fprintf(audio_file,"%c",audio_config[i]);
}

//pkt:packetStart  len:packetlen
void AudioFrameWrite_AMR(FILE *fp_aac,char *pkt, int len,const char *audio_config)
{
	fwrite((char *)(pkt+13),sizeof(char),len-13,fp_aac);
}

void AudioSDPAccept_G711(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_G711_track)
{
	*fileExt=PCMU;
}
void AudioFileHeaderWrite_G711(FILE *audio_file, const char *audio_config, const int audio_config_len)
{
	fwrite("orig_dam.ul",sizeof(char), 11 ,audio_file);//11 is the length for orig_dam.ul
}

//pkt:packetStart  len:packetlen
void AudioFrameWrite_G711(FILE *fp_aac,char *pkt, int len,const char *audio_config)
{
	fwrite((char *)(pkt+12),sizeof(char),len-12,fp_aac);//12 is rtp header length
}