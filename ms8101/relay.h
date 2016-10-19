#include "nctype.h"
#ifndef _RELAY_H_
#define _RELAY_H_


struct _relay_info;
///\brief 紀錄ncclient連到streaming server的方式
///
///連線的方式包含連線的ip, port, sdp傳送狀態, 串流傳送的策略
struct _streaming_server_info
{
	struct _relay_info *pserver;
	//policy object define for pserver
	///紀錄SDP傳送到relay server的狀態初始值為NO
	///當nc收到ncclient傳來的SDP後，需設為YES，代表SDP已接收完畢，可以送出TYPE_SDP訊息到sdpreceiver
	///sdpreceiver回傳TYPE_PORT後，設為OK
	u_int8_t sdp2relay;
	///紀錄nc傳送串流的通道初始化的狀態，初始值為IDLE
	///當nc收到sdpreceiver回傳TYPE_PORT，設為REDO，確認接收到正確的TYPE_PORT訊息後，設為SET_OK
	///代表通道已經初始化完畢
	u_int8_t cmdState;
	///cam是否需要forward串流到streaming server(使用在active/active policy)
	u_int8_t enable;
	
	///streaming server video port
	int video_udpport;
	///streaming server socket	
	int videoudpsockfd;
	int videoaddrlen;
	struct sockaddr_in videoaddress;  
	
	///streaming server audio port
	int audio_udpport;
	///streaming server socket	
	int audioudpsockfd;
	int audioaddrlen;
	struct sockaddr_in audioaddress; 
};


int set_relay_ip(int relaycnt, char *ip);
char * get_relay_ip(int relaycnt);
char *get_active_relay_ip();
int get_relay_port();
int get_relayNum();
int set_relayNum(int val);
int init_streaming_server_info(void *_infos);
int init_cam_streaming_server_info(void *_infos);
int free_streaming_server_info(void *_infos);
int forward_video_stream(void *_infos, unsigned char *stream_start, int streamlen);
int forward_audio_stream(void *_infos, unsigned char *stream_start, int streamlen);
int disable_forward_stream(void *_infos);
int set_sdp2relay(void *_infos, int val);

// Socket data type 
#define TYPE_SDP  		  0x0000
#define TYPE_PORT		  0x0001
#define TYPE_HEARTBEAT    0x0002
#define TYPE_ACK		  0x0003
#define TYPE_GETPUBIP     0x0004
#define TYPE_SETPUBIP     0x0005

#define BUFSIZE_DATA			1400
///\struct SOCKET_DATA_INFO_T relay.h relay.h
///\brief 處理sdpreceiver對nc的通訊訊息格式
///
///目前共定義三種傳輸交握<br>
///1. nc啟動時，連線sdpreceiver(relay server)，取得relay server的public ip後才算是連線成功
///\startuml
///            nc -> sdpreceiver: TYPE_GETPUBIP
///   sdpreceiver -> nc: TYPE_SETPUBIP
///\enduml
///2. cam連線到nc時，傳送sdp到sdpreceiver(relay server)，取得streaming server的傳送port
///\startuml
///            nc -> sdpreceiver: TYPE_SDP
///   sdpreceiver -> nc: TYPE_PORT
///\enduml
///3. nc和sdpreceiver互相檢查，服務是否正常
///\startuml
///            nc -> sdpreceiver: TYPE_HEARTBEAT
///   sdpreceiver -> nc: TYPE_ACK
///\enduml
///以sdpreceiver的實作功能而言，上面三個交握並沒有順序性
///只要nc connect上sdpreceiver且發出任意一個交握訊息成功後，sdpreceiver就會mount nc disk1
///但以nc的實作來說，有限定先執行TYPE_SETPUBIP, TYPE_SDP
typedef struct 
{
	///\brief通訊的命令種類 in big endian format
	///
	///TYPE_SDP          0x0000<br>
	///TYPE_PORT         0x0001<br>
	///TYPE_HEARTBEAT    0x0002<br>
	///TYPE_ACK			 0x0003<br>
	///TYPE_GETPUBIP     0x0004<br>
	///TYPE_SETPUBIP     0x0005<br>
	u16 dataType;
	///\brief payload的長度 in big endian format
	///
	///目前nc部分固定填入sizeof(int)=4，傳到sdpreceiver後，sdpreceiver未使用此欄位，而是採用字串結束字元來判斷封包結尾
	///sdpreceiver回傳nc時，回傳payload(data)的長度(目前sdpreceiver實作有問題，暫時不使用)
	u32 dataLen;
	///\brief 傳送本命令的時間 in epoch format
	u32 time;
	///\brief payload依據dataType傳送不同的內容
	///
	///dataType為TYPE_SDP傳sdp內容，sdp結尾需傳字串結尾字元'\0'<br>
	///dataType為TYPE_PORT, data[0] video port low byte, data[1] video port high byte, data[2] audio port low byte, data[3] audio port high byte<br>
	///dataType為TYPE_HEARTBEAT在data[0]傳'\0'<br>
	///dataType為TYPE_ACK不須傳資料<br>
	///dataType為TYPE_GETPUBIP在data[0]傳'\0'<br>
	///dataType為TYPE_SETPUBIP傳relay server的public ip in string format以'\0'結尾<br>	
	u8 data[BUFSIZE_DATA];
} SOCKET_DATA_INFO_T;	

void GetSendPacket(SOCKET_DATA_INFO_T *data, const int type, int channel);
void GetReadPacket(const SOCKET_DATA_INFO_T *data, int relayidx, int channel);
#endif
