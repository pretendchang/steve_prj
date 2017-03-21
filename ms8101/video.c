
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
#include "video.h"

#define CONFIG_LEN 	    640//128 //refers to main.h
#define BUFSIZE_LINE	1024//refers to main.h

#define YES 1
#define NO  0
///\addtogroup avapi
///@{
	
///\brief 處理video codec
///
///加入新video codec SOP
///1. 實作VideoHandling
///2. 新增video_handlers
///3. video.h定義新的VideoCodecType
///4. video.h更改MAX_VIDEO_CODEC值
struct VideoHandling
{
	///SDP中codec的名字
	char codename[10];
	///nc中存檔檔案的副檔名
	char fileextname[10];
	///\brief 依據傳入的sdp字串sdp_video_track，取得相關資訊
	///
	///依據傳入的sdp字串sdp_video_track，取得影像串流的編碼資料fileExt, video_config, video_config_len
	///fileExt為編碼的格式
	///\param fileExt 影片編碼的格式，格式號碼對應參考VideoCodecType
	///\param video_config 自sdp取得影片編碼的相關資訊，將會在VideoFrameWriteHandling和VideoFileHeaderWriteHandling兩函數中使用
	///\param video_config_len video_config的長度
	void (*VideoSDPAccept)(VideoU8* fileExt, VideoU8 *video_config, int *video_config_len, char *sdp_track);
	///寫入video檔案的檔頭
	///
	///寫入audio檔案的檔頭
	///\param video_file input，欲寫入檔案的FILE結構指標
	///\param video_config input，AudioSDPAccept回傳的audio_config
	///\param video_config_len input，AudioSDPAccept回傳的audio_config_len
	int (*VideoFileHeaderWrite)(FILE *video_file, const char *video_config, const int video_config_len);
	///\brief 將video串流，以frame為單位寫入檔案中
	///
	///由於全時錄影，H264串流需確認第一個寫入檔案的frame是iframe，因此WaitIFramEnable參數用來設定是否要等待第一個frame為IFrame才寫入<br>
	///若WaitIFramEnable為YES(1)，IsFirstPacket, IsIframe, WaitI須給正確的參數<br>
	///若WaitIFramEnable為NO(0)，IsFirstPacket, IsIframe, WaitI此三個值不會使用，可分別設定為0,0,NULL
	///\param fileext video的格式，參考AssertVideoCodecType
	///\param fp 要被寫入串流資料的檔案fd
	///\param pkt 串流資料
	///\param len 串流資料長度
	///\param video_config 由VideoSDPAcceptHandling取得之video_config，自sdp取得的相關串流資訊，將會被運算後寫入檔案
	///\param WaitIFramEnable 設定是否要等待第一個frame為IFrame才寫入
	///\param IsFirstPacket client.firstPacket
	///\param IsIframe VideoCheckIFrameHandling的回傳值，檢查該frame是否為iframe
	///\param WaitI client.wait264I
	int (*VideoFrameWrite)(FILE *fp,char *pkt, int len,const VideoU8 *video_config, const VideoU8 WaitIFramEnable, const VideoU8 IsFirstPacket, const VideoU8 IsIframe, VideoU8 *WaitI);
	///檢查frame pkt是否為iframe
	int (*VideoCheckIFrame)(char *pkt);
};
///@}
void VideoSDPAccept_M4V(VideoU8* fileExt, VideoU8 *video_config, int *video_config_len, char *sdp_M4V_track);
int VideoFileHeaderWrite_M4V(FILE *video_file, const char *video_config, const int video_config_len);
int VideoFrameWrite_M4V(FILE *fp,char *pkt, int len,const VideoU8 *video_config, const VideoU8 WaitIFramEnable, const VideoU8 IsFirstPacket, const VideoU8 IsIframe, VideoU8 *WaitI);
int checkI_M4V(char *pkt);

void VideoSDPAccept_H264(VideoU8* fileExt, VideoU8 *video_config, int *video_config_len, char *sdp_M4V_track);
int VideoFileHeaderWrite_H264(FILE *video_file, const char *video_config, const int video_config_len);
int VideoFrameWrite_H264(FILE *fp,char *pkt, int len,const VideoU8 *video_config, const VideoU8 WaitIFramEnable, const VideoU8 IsFirstPacket, const VideoU8 IsIframe, VideoU8 *WaitI);
int checkI_H264(char *pkt);
///\addtogroup avapi
///@{
	
///VideoHandling實作陣列
struct VideoHandling video_handlers[]=
{
	{"M4V",  "m4v",  VideoSDPAccept_M4V,   VideoFileHeaderWrite_M4V,  VideoFrameWrite_M4V,  checkI_M4V},
	{"H264", "264",  VideoSDPAccept_H264,  VideoFileHeaderWrite_H264, VideoFrameWrite_H264, checkI_H264}
};

///\brief 依照輸入的fileext，檢查video codec編碼代號是否合法
///
///檢查video codec編碼代號是否合法
///\param fileext input，video codec編碼代號
int AssertVideoCodecType(const int fileext)
{
	if(fileext<M4V || fileext>MAX_VIDEO_CODEC)
		return VIDEO_NOK;
	return VIDEO_OK;
}
///@}
char* VideoFileExtHandling(int fileext)
{
	if(AssertVideoCodecType(fileext)==VIDEO_OK)
		return &(video_handlers[fileext].fileextname[0]);
	else
		printf("Wrong VideoCodecType:%d",fileext);
	return NULL;
}
int VideoSDPAcceptHandling(VideoU8* fileExt, VideoU8 *video_config, int *video_config_len, char *sdp_video_track)
{
	char *sdp_codec_track;
	int i;
	for(i=0;i<sizeof(video_handlers)/sizeof(struct VideoHandling);i++)
	{
		sdp_codec_track = strstr(sdp_video_track, video_handlers[i].codename);
		if(sdp_codec_track !=NULL)
		{
			video_handlers[i].VideoSDPAccept(fileExt, video_config, video_config_len, sdp_codec_track);
			return VIDEO_OK;
		}
	}
	return VIDEO_NOK;
}
int VideoFileHeaderWriteHandling(const int fileext, FILE *video_file, const char *video_config, const int video_config_len)
{
	if(AssertVideoCodecType(fileext)==VIDEO_OK)
		return video_handlers[fileext].VideoFileHeaderWrite(video_file, video_config, video_config_len);
	else
	{
		printf("Wrong VideoCodecType:%d",fileext);
		return VIDEO_NOK;
	}
}

int VideoFrameWriteHandling(const int fileext, FILE *fp,char *pkt, int len,const VideoU8 *video_config, const VideoU8 WaitIFramEnable, const VideoU8 IsFirstPacket, const VideoU8 IsIframe, VideoU8 *WaitI)
{
	if(AssertVideoCodecType(fileext)==VIDEO_OK)
	{
		return video_handlers[fileext].VideoFrameWrite(fp, pkt, len, video_config, WaitIFramEnable, IsFirstPacket, IsIframe, WaitI);
	}
	else
	{
		printf("Wrong VideoCodecType:%d",fileext);
		return VIDEO_NOK;
	}
}
int VideoCheckIFrameHandling(char *pkt,int fileext)
{
	if(AssertVideoCodecType(fileext)==VIDEO_OK)
		return video_handlers[fileext].VideoCheckIFrame(pkt);
	else
	{
		printf("Wrong AudioCodecType:%d",fileext);
		return VIDEO_NOK;
	}
}
void VideoSDPAccept_M4V(VideoU8* fileExt, VideoU8 *video_config, int *video_config_len, char *sdp_M4V_track)
{
	char *q;
	unsigned char ch, ch2, val;
	int i;
	
	*fileExt=M4V;
	q = strstr(sdp_M4V_track, "config=");
	if(q!=NULL)
	{
		q+=7;
		for (i=0; *q && *q!=';' && *q!='\n' && *q!='\r' && i<CONFIG_LEN; q+=2)
		{
			ch=*q;
			ch2=*(q+1);

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
			video_config[i++]= val;

		}
		*video_config_len=i;
	}
}

int VideoFileHeaderWrite_M4V(FILE *video_file, const char *video_config, const int video_config_len)
{
	int i;
	for(i=0; i<video_config_len; i++)
		fprintf(video_file,"%c",video_config[i]);
	return VIDEO_OK;
}
int VideoFrameWrite_M4V(FILE *fp,char *pkt, int len,const VideoU8 *video_config, const VideoU8 WaitIFramEnable, const VideoU8 IsFirstPacket, const VideoU8 IsIframe, VideoU8 *WaitI)
{
	fwrite((char *)(pkt+12),sizeof(char), len-12, fp);
	return VIDEO_OK;
}
#define RTP_HEADER_LEN 12 //refers to main.h
int checkI_M4V(char *pkt)
{
	unsigned char *p;
	p=(unsigned char *)pkt;
	if( *(p+RTP_HEADER_LEN)==0x00 && *(p+RTP_HEADER_LEN+1)==0x00 && *(p+RTP_HEADER_LEN+2)==0x01 && *(p+RTP_HEADER_LEN+3)==0xb6 && (*(p+RTP_HEADER_LEN+4)&0xf0)<0x50 )
		return VIDEO_OK;
	else
		return VIDEO_NOK;
}

static char base64_table[255];
void base64_tableinit()
{
    int i,j;

    bzero(base64_table,255);

    for(j=0,i='A'; i<='Z'; i++)
        base64_table[i]=j++;
    for(i='a'; i<='z'; i++)
        base64_table[i]=j++;
    for(i='0'; i<='9'; i++)
        base64_table[i]=j++;

    base64_table['+']=j++;
    base64_table['/']=j++;
    base64_table['=']=j;
}

char *decode(const char *cptr,char **rptr,int *tmpLen)
{
    char *res;
    int clen,len,i;
    static int init=0;

    if(cptr==NULL)
        return NULL;

    len=strlen(cptr);

    if(len%4)
        return NULL;

    if(!init)
    {
        base64_tableinit();
        init=1;
    }

    clen=len/4;

    if((res=malloc(len-clen))==NULL)
        return NULL;

    *tmpLen=len-clen;
    for(i=0; i<len; i++)
        if(*(cptr+i)=='=')
            *tmpLen=*tmpLen-1;

    for(*rptr=res; clen--;)
    {
        *res=base64_table[(int)*cptr++]<<2&0xfc;
        *res++|=base64_table[(int)*cptr]>>4;
        *res=base64_table[(int)*cptr++]<<4&0xf0;
        *res++|=base64_table[(int)*cptr]>>2&0x0f;
        *res=base64_table[(int)*cptr++]<<6;

        if(*cptr!='=')
            *res++|=base64_table[(int)*cptr++];
    }
    return *rptr;
}

void setH264Head(VideoU8 *video_config, int *video_config_len)
{
    int len;

    len=*video_config_len;
    video_config[len++]=0x00;
    video_config[len++]=0x00;
    video_config[len++]=0x00;
    video_config[len++]=0x01;
    *video_config_len=len;
}

void setH264Config(VideoU8 *video_config, int *video_config_len,char *str1,char *str2)
{
    char *b64Ptr=NULL;
    int i,len,tmpLen;

    setH264Head(video_config, video_config_len);

    b64Ptr=decode(str1,&b64Ptr,&tmpLen);

    if(b64Ptr!=NULL)
    {
        len=*video_config_len;
        for(i=0; i<tmpLen; i++,(*video_config_len)++)
        {
            video_config[i+len]=*(b64Ptr+i);
        }
        setH264Head(video_config, video_config_len);

        b64Ptr=decode(str2,&b64Ptr,&tmpLen);

        if(b64Ptr!=NULL)
        {
            len=*video_config_len;
            for(i=0; i<tmpLen; i++,(*video_config_len)++)
            {
                video_config[i+len]=*(b64Ptr+i);
            }
            setH264Head(video_config, video_config_len);
        }
    }
}

int output_h264(FILE *fp,char *pkt, int len)
{
    int offset = 0;
    char startCode[4] = {0x00, 0x00, 0x00, 0x01};
    int byteWrite, toWrite;
    char nalHdr;


    if((pkt[offset + 12] & 0x1f) == 0x1c)
    {
        // FU-A
        if((pkt[offset + 13] & 0x80) == 0x80)
        {
            nalHdr = (pkt[offset + 12] & 0xe0) | (pkt[offset + 13] & 0x1f);

            byteWrite = fwrite(&nalHdr, 1, 1, fp);
            if(byteWrite != 1)
            {
                printf("file write failed\n");
                return byteWrite;
            }
        }
        toWrite = len - offset - 12 - 2;
        byteWrite = fwrite(&pkt[offset + 12 + 2], 1, toWrite, fp);
        if(toWrite != byteWrite)
        {
            printf("file write failed\n");
            return byteWrite;
        }
    }
    else
    {
        nalHdr = pkt[offset + 12];
        byteWrite = fwrite(&nalHdr, 1, 1, fp);
        if(byteWrite != 1)
        {
            printf("file write failed\n");
            return byteWrite;
        }
        toWrite = len - offset - 12 - 1;
        byteWrite = fwrite(&pkt[offset + 12 + 1], 1, toWrite, fp);
        if(toWrite != byteWrite)
        {
            printf("file write failed\n");
            return byteWrite;
        }
    }

    if((pkt[offset + 1] & 0x80) == 0x80)
    {
        byteWrite = fwrite(startCode, 1, 4, fp);
        if(byteWrite != 4)
        {
            printf("file write failed\n");
            return byteWrite;
        }
    }
    return len;
}

void VideoSDPAccept_H264(VideoU8* fileExt, VideoU8 *video_config, int *video_config_len, char *sdp_H264_track)
{
	char *r;
	char tmpBuf1[BUFSIZE_LINE],tmpBuf2[BUFSIZE_LINE];
	int i;
	
	*fileExt=H264;
	r = strstr(sdp_H264_track, "sprop-parameter-sets=");
	if(r!=NULL)
	{
		r+=21;
		tmpBuf1[0]=tmpBuf2[0]='\0';
		for (i=0; *r && *r!=',' && *r!=';' && *r!='\n' && *r!='\r' && i<CONFIG_LEN; r++)
			tmpBuf1[i++]=*r;
		tmpBuf1[i]='\0';
		if(*r==',')
		{
			r++;
			for (i=0; *r && *r!=',' && *r!=';' && *r!='\n' && *r!='\r' && i<CONFIG_LEN; r++)
				tmpBuf2[i++]=*r;
			tmpBuf2[i]='\0';
		}

		if(strlen(tmpBuf1)>0)
			setH264Config(video_config, video_config_len,tmpBuf1,tmpBuf2);
	}
}

int VideoFileHeaderWrite_H264(FILE *video_file, const char *video_config, const int video_config_len)
{
	int i;
	for(i=0; i<video_config_len; i++)
		fprintf(video_file,"%c",video_config[i]);
	return VIDEO_OK;
}
int VideoFrameWrite_H264(FILE *fp, char *pkt, int len, const VideoU8 *video_config, 
							const VideoU8 WaitIFramEnable, 
							const VideoU8 IsFirstPacket, 
							const VideoU8 IsIframe, VideoU8 *WaitI)
{
	if(WaitIFramEnable==0)
	{
		return output_h264(fp, pkt, len);
	}
	else
	{
		if(IsFirstPacket==YES && IsIframe==0)
		{
			*WaitI=YES;
			printf("first video packet is not I frame\n");
		}

		if((*WaitI)==YES && IsIframe==1)
		{
			*WaitI=NO;
			printf("get I frame\n");
		}
		if((*WaitI)==NO)
		{
			//if(client[index].stevedbgtid!=syscall(SYS_gettid) || client[index].stevedbgfptr!=(long)client[index].fptr || strcmp(client[index].stevedbgcamid,client[index].camid)!=0)
			//stevedebug(debugMsg,"wait264I=NO:%d,%ld,%lx,%s",index,syscall(SYS_gettid),(unsigned long)client[index].fptr,client[index].camid);
			return output_h264(fp, pkt, len);//check fptr
		}
		return VIDEO_NOK;
	}
}

int checkI_H264(char *pkt)
{
	char nalHdr;

	if((pkt[12] & 0x1f) == 0x1c)
	{
		if((pkt[13] & 0x80) == 0x80)
		{
			nalHdr = (pkt[12] & 0xe0) | (pkt[13] & 0x1f);

			if(nalHdr==0x65 || nalHdr==0x25)
				return VIDEO_OK;
			else
				return VIDEO_NOK;
		}
		else
			return VIDEO_NOK;
	}
	else
	{
		nalHdr = pkt[12];
		if(nalHdr==0x65)
			return VIDEO_OK;
		else
			return VIDEO_NOK;
	}
}