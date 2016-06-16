#ifndef _MS8101
#define _MS8101

#define TO_VIDEO_T 0x0000
#define TO_AUDIO_T 0x1000
#define TO_PICTURE_T 0x2000
#define TO_CONFIG_8_T 0xa000
#define TO_CONFIG_4_T 0xb000
#define TO_TIME_T 0xc000
#define TO_CONFIG_1_T 0xd000
#define TO_EVENT_T 0xe000
#define TO_SDP_T 0xf000
#define TCPBUFFERSIZE 4096
#define SIZEOF_TCPIPHEADER 40
#define TO_NC_HEADER_LEN 2
#include <string.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <time.h>
#include <errno.h>

void print_hexdump(char *data, int len)
{
    int line;
    int max_lines = (len / 16) + (len % 16 == 0 ? 0 : 1);
    int i;

    for(line = 0; line < max_lines; line++)
    {
        printf("%08x  ", line * 16);

        /* print hex */
        for(i = line * 16; i < (8 + (line * 16)); i++)
        {
            if(i < len)
                printf("%02x ", (uint8_t)data[i]);
            else
                printf("   ");
        }
        printf(" ");
        for(i = (line * 16) + 8; i < (16 + (line * 16)); i++)
        {
            if(i < len)
                printf("%02x ", (uint8_t)data[i]);
            else
                printf("   ");
        }

        printf(" ");

        /* print ascii */
        for(i = line * 16; i < (8 + (line * 16)); i++)
        {
            if(i < len)
            {
                if(32 <= data[i] && data[i] <= 126)
                    printf("%c", data[i]);
                else
                    printf(".");
            }
            else
                printf(" ");
        }
        printf(" ");
        for(i = (line * 16) + 8; i < (16 + (line * 16)); i++)
        {
            if(i < len)
            {
                if(32 <= data[i] && data[i] <= 126)
                    printf("%c", data[i]);
                else
                    printf(".");
            }
            else
                printf(" ");
        }

        printf("\n");
    }
}
//util stuff
#define ElementCountInArray(a) (sizeof(a)/sizeof(a[0]))
#define SWAP2BYTES(x) ((((x)>>8)&0xff)|(((x)<<8)&0xff00))

typedef unsigned char U8;
typedef unsigned short U16;
typedef unsigned int U32;

//module return define
#define MS8101_NOK 0
#define MS8101_OK  1

struct RTP_FRAME
{
	U8 RTP_HEADER[12];
	U8 RTP_PAYLOAD[1446];
};

struct SHORT_MSG
{
	U8 MSG[3];//SYN  or  ACK
};

struct Event_PACKET_Part2
{
	U8 MovieStartTime[10];
	U8 MovieStartTime_END;

	U8 GlobalStatus[11];

	U8 SensorStatus[416];
	U8 SensorStatus_End;

	U8 IPCAM_Version[9];

	U8 UPLinkBitRate[5];

	U8 MemoryUsage[4];

	U8 SERVICE_ID[12];

	U8 Function_Type[3];

	U8 Packet_Type[3];

	U8 Manufacturer[3];

	U8 Model[3];

	U8 Camera_Status[3];

	U8 CPE_ID[24];
};

struct Event_PACKET_Part1
{
	U8 Event_Keyword[9];
	U8 Event_Keyword_END;

	U8 StatusTime[10];
	U8 StatusTime_END;

	U8 Camid[15];//可變長度 max is 15
	U8 Camid_END;
};

//MS8101 Event_PACKET_Keyword define
#define EVENT_SYN       "SYN"
#define SYN_PACKETLEN strlen(EVENT_SYN)

#define EVENT_CAM_EVENT "CAM_EVENT"
#define EVENT_CAM_RSDVD "CAM_RSDVD"
#define EVENT_CAM_SAVE1 "CAM_SAVE1"
#define EVENT_IPCAM1    "IPCAM1"
#define EVENT_DVR_MV    "DVR_MV"  //isDVR設為true，ref main.h isDVR
#define EVENT_CAM_VIDEO "CAM_VIDEO"
#define EVENT_CAM_JPGLV "CAM_JPGLV"
#define EVENT_CAM_JPGEV "CAM_JPGEV"
#define EVENT_CAM_COMMD "CAM_COMMD"

//MS8101 CAM_COMMD define
#define CAM_COMMD_GET_CONFIG    "0512"
#define CAM_COMMD_0270          "0270"
#define CAM_COMMD_DISABLE_VIDEO "0261"
#define CAM_COMMD_ENABLE_VIDEO  "0260"


//MS8101 protocol util
#define MS8101_HEADER_EVENT_TYPE(U16PACKET_HEADER)      (((U16PACKET_HEADER)>>4)&0xf)
#define MS8101_HEADER_PAYLOAD_LENGTH(U16PACKET_HEADER)  ((((U16PACKET_HEADER)>>8)&0xff)|(((U16PACKET_HEADER)&0xf)<<8))
#define ENABLE_RECORDING(MOVIE_START_TIME) MOVIE_START_TIME[0]!='0'

#define PACKET_SPLITTER 0x20
#define PACKET_END 0
#define PACKET_SETVALUE(MEMBER,format,...) sprintf(MEMBER,format,##__VA_ARGS__);MEMBER##_END=PACKET_SPLITTER;
#define SET_PACKET_END(MEMBER)   MEMBER##_END=PACKET_END

#define PACKET_GETVALUE_REF(MEMBER) MEMBER  
//modify the original membuf and set \0 at the end of the string

int PACKET_GETVALUE_IMPL(const char *packet, char *result, const int size)
{
	memcpy(result, packet, size-1);
	result[size-1]=0;
}
#define PACKET_GETVALUE_NEW(MEMBER, MEMBUF) PACKET_GETVALUE_IMPL(MEMBER,MEMBUF,ElementCountInArray(MEMBER))  
//copy the value to a new membuf and set \0 at the end of the string
//MS8101 protocol util

struct Event_PACKET_Part2 * SET_PACKET_CAMID_END_AND_LINK_PART2(struct Event_PACKET_Part1 *packet)
{
	char *camidend = strchr((char*)packet->Camid, PACKET_SPLITTER);
	*camidend=0;
	return (struct Event_PACKET_Part2 *)(camidend+1);
}

struct Event_PACKET_Part2 * SET_PACKET_CAMID_VALUE_AND_LINK_PART2(struct Event_PACKET_Part1 *packet, char *format,...)
{
	va_list		ap;
    va_start(ap, format);
	vsprintf((char*)packet->Camid, format, ap);
	va_end(ap);
	char *camidend = &packet->Camid[strlen((char*)packet->Camid)];
	*camidend=PACKET_SPLITTER;
	return (struct Event_PACKET_Part2 *)(camidend+1);
}

struct ms8101_header_bit
{
	U8  command:4;
	U16 length:12;
};

#pragma pack(push)
#pragma pack(1)
struct ms8101_t
{
	union ms8101_header
	{
		U8  U8Packet_Header[2];
		U16 U16Packet_Header;
		struct ms8101_header_bit bitfield;		
	}ms8101_header_t;
	union ms8101_payload
	{
		struct RTP_FRAME frame;
		struct SHORT_MSG msg;
		struct Event_PACKET_Part1 event;
		U8 data[TCPBUFFERSIZE];
	}ms8101_payload_t;
};
#pragma pack(pop)

int send_cam_save1_start(int i);//ms8101
int send_to_nc(int i, int msg_type, const void *buf, size_t len);//ms8101
#endif