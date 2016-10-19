#ifndef _MS8101
#define _MS8101

//for ncclient send command to nc
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


void print_hexdump(char *data, int len);
//util stuff
#define ElementCountInArray(a) (sizeof(a)/sizeof(a[0]))
#define SWAP2BYTES(x) ((((x)>>8)&0xff)|(((x)<<8)&0xff00))

typedef unsigned char U8;
typedef unsigned short U16;
typedef unsigned int U32;

//module return define
#define MS8101_NOK 0
#define MS8101_OK  1
/*
//for nc client acks to nc
#define ACK_I         0x0000
#define ACK_SDP       0x0001
#define ACK_SYN		  0x0002
#define ACK_EVENT     0x0003
#define ACK_VSTART    0x0004
#define ACK_VSTOP     0x0005
#define ACK_CONFIG    0x0007
#define ACK_TIME      0x0008
#define ACK_SSTART    0x0009//CAM_SAVE1
#define ACK_SSTOP     0x000a
#define ACK_RSTART    0x000d//CAM_RSDVD
#define ACK_RSTOP     0x000e
#define ACK_JLVSTART  0x0018
#define ACK_JLVSTOP   0x0019
#define ACK_JEVSTART  0x001a
#define ACK_JEVSTOP   0x001b
#define ACK_MAX       0x0020
*/
// cmd 255-511 legacy ipcam command, not implement
#define ACK_ARM_MODE         0x0100 //256
#define ACK_SILENT_ARM_MODE  0x0101 //257
#define ACK_DISARM_MODE      0x0102 //258
#define ACK_MAINTAIN_MODE    0x0103 //259
#define ACK_VIDEO_ENABLE     0x0104 //260
#define ACK_VIDEO_DISABLE    0x0105 //261
#define ACK_SCHEDULE_VIDEO_ENABLE     0x0108 //264
#define ACK_SCHEDULE_VIDEO_DISABLE    0x0109 //265
#define ACK_PANIC_MODE       0x010a //266
#define ACK_GUARDING_MODE    0x010b //267
#define ACK_REQUEST_LOCAL_STORAGE_VIDEO       0x010c //268
#define ACK_UNMOUNT_STORAGR    0x010d //269

// cmd 512-767 cpe reboot, config
#define ACK_GETCONFIG   0x0200  //512
#define ACK_SETCONFIG   0x0201  //513
#define ACK_UPGRADE     0x0202  //514
#define ACK_NTPDATE     0x0203  //515
#define ACK_REBOOT      0x0204  //516
#define ACK_TEST        0x0205  //517
#define ACK_ALARM       0x0206  //518
#define ACK_RTSP_MOD    0x0207  //519
#define ACK_MOTION_AREA 0x0208  //520
#define ACK_MOTION_SENSITIVITY        0x0209  //521
#define ACK_MOTION_THRESHOLD          0x020a  //522
#define ACK_CAM_STATE   0x020b  //523
#define ACK_CONTROL_OUT 0x020c  //524
#define ACK_DIO_CATEGORY    0x020d  //525
#define ACK_STORAGE_INFO    0x020e  //526
#define ACK_CHECK_STORAGE_FILE  0x020f  // 527
#define ACK_AUDIO_OUT   0x0210  // 528
#define ACK_AUDIO_IN    0x0211  // 529

//MS8101 Event_PACKET_Keyword define
#define EVENT_SYN       "SYN"
#define SYN_PACKETLEN strlen(EVENT_SYN)

#define EVENT_CAM_EVENT "CAM_EVENT"
#define EVENT_CAM_RSDVD "CAM_RSDVD"
#define EVENT_CAM_SAVE1 "CAM_SAVE1"
#define EVENT_IPCAM1    "IPCAM1"
#define EVENT_DVR_MV    "DVR_MV"  ///isDVR設為true，ref main.h isDVR
#define EVENT_CAM_VIDEO "CAM_VIDEO"
#define EVENT_CAM_JPGLV "CAM_JPGLV"
#define EVENT_CAM_JPGEV "CAM_JPGEV"
#define EVENT_CAM_COMMD "CAM_COMMD"

//MS8101 CAM_COMMD define
#define CAM_COMMD_GET_CONFIG    "0512"
#define CAM_COMMD_SET_CONFIG    "0513"
#define CAM_COMMD_0270          "0270"
#define CAM_COMMD_DISABLE_VIDEO "0261"
#define CAM_COMMD_ENABLE_VIDEO  "0260"


//MS8101 protocol util
//#define MS8101_HEADER_EVENT_TYPE(U16PACKET_HEADER)      (((U16PACKET_HEADER)>>4)&0xf)
//#define MS8101_HEADER_PAYLOAD_LENGTH(U16PACKET_HEADER)  ((((U16PACKET_HEADER)>>8)&0xff)|(((U16PACKET_HEADER)&0xf)<<8))





//nc server api
///取得目前ms8101_command_handler陣列的元素數目
int GetCount_ms8101_command_handler(struct _nc_controller_protocol *protocol);
///檢查ncclient傳來的命令，是否為開始錄影
#define ENABLE_RECORDING(MOVIE_START_TIME) MOVIE_START_TIME[0]!='0'

#define PACKET_SPLITTER 0x20
#define PACKET_END 0
#define PACKET_SETVALUE(MEMBER,format,...) sprintf(MEMBER,format,##__VA_ARGS__);MEMBER##_END=PACKET_SPLITTER;
#define SET_PACKET_END(MEMBER)   MEMBER##_END=PACKET_END

#define PACKET_GETVALUE_REF(MEMBER) MEMBER  
//modify the original membuf and set \0 at the end of the string

int PACKET_GETVALUE_IMPL(const char *packet, char *result, const int size);
#define PACKET_GETVALUE_NEW(MEMBER, MEMBUF) PACKET_GETVALUE_IMPL(MEMBER,MEMBUF,ElementCountInArray(MEMBER))  
//copy the value to a new membuf and set \0 at the end of the string

struct Event_PACKET_Part1;
struct Event_PACKET_Part2;
struct Event_PACKET_Part2 * SET_PACKET_CAMID_END_AND_LINK_PART2(struct Event_PACKET_Part1 *packet);
struct Event_PACKET_Part2 * SET_PACKET_CAMID_VALUE_AND_LINK_PART2(struct Event_PACKET_Part1 *packet, char *format,...);

//ncclient api
int send_cam_commd(int i, int cmd);
int send_cam_event1(int i);
int send_cam_event2(int i, int movieStartTime);
int send_cam_event_all();
int send_syn(int i);
int send_cam_save1_start(int i);
int send_to_nc(int i, int msg_type, const void *buf, size_t len);
int set_maxchannel(int i);//maxchannel used in send_cam_event_all

//nc sends to dvr
struct ms8101_header_bit
{
	U16 length:12;
	U8  command:4;
};//little endian

#pragma pack(push)
#pragma pack(1)

#include "../rtp.h"
struct SHORT_MSG
{
	U8 MSG[3];//SYN  or  ACK
};

///\struct Event_PACKET_Part1 ms8101.h ms8101.h
///\brief ms8101協定中的event命令格式
///
///由於camid欄位長度不固定，EVENT_PACKET拆解成兩段：Event_PACKET_Part1和Event_PACKET_Part2
///資料接收完畢後，呼叫SET_PACKET_CAMID_END_AND_LINK_PART2，即可把Event_PACKET_Part2接上Event_PACKET_Part1後
struct Event_PACKET_Part1
{
	U8 Event_Keyword[9];
	U8 Event_Keyword_END;

	U8 StatusTime[10];
	U8 StatusTime_END;

	U8 Camid[15];///可變長度 max is 15
	U8 Camid_END;
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

#define CAMID_LEN       20
///\brief ipcam_client執行toipcam_request命令傳送的資料格式
///
///network transfer in little endian format
///nc acks to toipcam_client camid=camxxxxxxx\\0\\ncode=camCommand\\nOK\\0
struct camcom_t 
{
    char  camid[CAMID_LEN];
    U16 command;   
    char  data[1024];   
};


#define PACKET_LEN	    1500
///\brief nc執行send_cam命令傳送的資料格式
///
///network transfer camCommandLen and camcom_t_command in big endian
union camCommand_t
{
	struct _camCommand_t_desc
	{
		U8 camCommandLenHB;
		U8 camCommandLenLB;
		U8 camcom_t_commandHB;
		U8 camcom_t_commandLB;
		U8 camCommand_Data;
	}camCommand_t_desc;
	U8 camCommand[PACKET_LEN];
};

//dvr sends or acks after camCommand_t
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
};//network transfer U8Packet_Header in big endian
#pragma pack(pop)
#endif