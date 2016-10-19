/*todo 每個訊息對應的nc_controller實作和refactor*/
#include "main.h"
#ifndef _NC_CONTROLLER_H_
#define _NC_CONTROLLER_H_
#define CONTROLLER_NOK 0
#define CONTROLLER_OK 1

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

#define Protocol_Plugin __attribute__((section(".protocol_module")))

///\brief Protocol_Command類型
///
///各類型command差別在於
///1. standard command用來監控個別攝影機，maintain command用來監控整個ncs服務的狀況
///2. standard command執行command前，將先執行default handler
///3. standard command比maintain command多出send_cam和cam_resp兩個跟nc_client通訊的程序
///4. STANDARD_IPCAM為standard command變形，只有toipcam_request和toipcam_ack兩個程序
enum _Protocol_Command_Type {
	STANDARD,
	MAINTAIN,
	STANDARD_IPCAM
};
///\defgroup ms8101test1 testing function algorithm
///@{
///@}


///\struct _MS8101_Command ms8101.h ms8101.h
///\brief 處理外部系統(ipcam_client)對nc的ms8101協定通訊
///
///前提：
///1. nc已收到該channel攝影機的SDP\n
///主流程：
///1. ipcam_client送出稍後要送出的camcom_t長度(封包長度:2 byte)
///2. ipcam_client送出toipcam_request camcom_t
///3. nc.toipcam回應toipcam_ack to ipcam_client camid=camxxxxxxx
///4. nc在toipcam處理camcom_t封包
///5. nc.tcpin 送出send_cam camcom到ncclient
///6. ncclient送出cam_resp camcommand(in ms8101_t)
///7. nc在tcpin處理該cam_resp camcommand
///8. nc把camcommand dequeue到event queue
///9. 若步驟五ack是0xd config，nc會把此封包再傳到toipcam_client
///10. 若有執行步驟9 toipcam_client會再ack訊息給nc，由步驟1開始ack訊息的流程到步驟8<p>
///新增ipcam功能實作方式：
///1. 程式碼需include ms8101Impl.h
///2. 定義實作新的_MS8101_Command物件
///3. 將該物件設定於ms8101_command_handlers，沒有實作的function則定義NULL，handler檢查函式指標為NULL時，僅會執行預設的行為
///\snippet ms8101_command_handlers.h ms8101_command_handlers_label
struct _Protocol_Command
{
	enum _Protocol_Command_Type type;
	int commandid;
	char cam_comm[10];
	
	///\brief 處理由toipcam client送來的camcom封包
	///
	///\param camcom toipcam client送來的camcom封包
	///\param ulen toipcam client送來的camcom封包的長度，? byte
	///\param channel toipcam client送來的封包送到哪個攝影機處理，該攝影機所在的clients index
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK
	///\ingroup ms8101test1
	int (*toipcam_request)(void *camcom,u_int16_t ulen, int channel);
	
	///\brief 接收到由toipcam client送來的camcom封包後，回傳訊息
	///
	///系統預設回傳無data資料的  
	///若要修改回傳的內容，修改msg，實作toipcam_ack_xxx並加到ms8101_command_handlers中
	///\param camcom toipcam client送來的camcom封包
	///\param ulen toipcam client送來的camcom封包的長度，? byte
	///\param msg 回傳給toipcam client的訊息
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK	
	int (*toipcam_ack)(void *camcom, u_int16_t ulen, char *msg);
	
	///\brief 接收到由toipcam client送來的camcom命令後，nc將此命令發送到nc_client
	///
	///系統預設回傳不帶data的camCommand_t結構\n
	///若要增加data欄位的內容，需實作send_cam_xxx並加到ms8101_command_handlers中
	///\param camcom_t 送到nc_client的送camcom封包
	///\param ulen 送到nc_client的送camcom_t封包的長度，? byte
	///\param channel nc_client的channel號碼
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK
	int (*send_cam)(void *camcom, u_int16_t ulen, int channel);
	
	///\brief nc_client接收到send_cam命令後，回傳給nc的封包處理函式
	///
	///\param channel nc_client的channel號碼
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK
	int (*cam_resp)(int channel);
};


///\brief nc_controller_variable_metadata
struct _nc_controller_variable_metadata
{
	char name[16];
	int  size;
};

///\brief _nc_controller_variable
struct _nc_controller_variable
{
	struct _nc_controller_variable_metadata *metadata;
	///define_variable函數中，以malloc初始化空間，系統結束時釋放記憶體
	long *ptr;
};
///\brief ncclient到nc的通訊介面protocol格式
///
///nc_controller執行_nc_controller_protocol次序關係如下
///\startuml
///main -> main:system_init
///group by camera channel
///main -> main:read_config_file
///create thread
///main -> thread:entry_point
///thread -> tcpin:cmdProcress
///tcpin -> tcpin:set_nc_controller_input
///alt receiveHeader=0x0 or 0x1
///tcpin -> AV:AV_data_handling
///alt Get an IFrame 
///AV -> AV :txResp
///end
///else receiveHeader=0xc
///tcpin -> time:txResp
///else receiveHeader=0xd
///tcpin -> config:txResp
///config -> config:cam_resp
///else receiveHeader=0xe
///tcpin -> event:event_packet_handling
///event -> event:check_packet_format
///event -> event:get_event_info
///alt SYN
///event -> event:txResp
///end
///event -> eventtodo:send_eventmsg_to_queue
///alt CAM_EVENT
///event -> event:txResp
///else 全時錄影或事件錄影
///event -> event:check_start_recording
///event -> event:txResp
///else CAM_JPGLV
///event -> event:txResp
///else CAM_JPGEV
///event -> event:txResp
///else CAM_COMMD
///event -> event:cam_resp
///end
///else receiveHeader=0xf
///tcpin -> sdp:sdp_accept
///sdp -> sdp:txResp
///end
///end
///main -> main:system_free
///\enduml
///\verbatim
///實作SOP
///1. 實作_nc_controller_protocol結構所有函數，函數以static定義，避免命名衝突
///2. 宣告步驟1結構為Protocol_Plugin，指定Plugin的初始值
///nc_controller架構
///payload header + payload
///payload header = payload receive_header + packet_length
///
///event架構
///event_keyword 參考nc_controller
///event_statustime:事件觸發時網路攝影機當時的epoch 時間
///event_moviestarttime:video 第一個 frame 的 epoch 時間 (也是印在影像上的嵌入時間)，如果沒有 event 發生或是沒有 video stream 即填入“0”。當 MovieStartTime 不為 0，則表示該 video 須要錄影
///\endverbatim
struct _nc_controller_protocol
{
	///\brief nc_controller的cmdProcress的初始化函數
	///
	///\verbatim
	///為cmdProcress執行後，第一個被呼叫的函數
	///需實作功能，如下
	///1. payload receive_header設定在client[channel].receiveHeader
	///2. 檢查封包的標頭是否接收完畢
	///3. packet_length設定在client[channel].packetLength(in byte)
	///4. 設定client[channel].packetStart在payload第一個byte
	///5. payload receive_header: payload封包種類
	///  5-1. 0x0 傳送video封包，in RTP format
	///  5-2. 0x1 傳送audio封包 in RTP format
	///  5-3. 0x2 ?
	///  5-4. 0xf SDP 傳送SDP封包，payload為完整SDP內容
	///  5-5. 0xe event，payload為完整event內容，參考event架構
	///  5-6. 0xd config，payload為稍後傳回ipcam client的內容
	///6. 若要使用nc_controller.cmdProcress功能，需指定client[index].bufferPtr位址到封包尾端
	///\endverbatim
	///範例程式
	///\snippet nc_ms8101.c set_nc_controller_input_ms8101_label
	int (*set_nc_controller_input)(const int channel, const void *packet);
	///\brief protocol處理函數的進入點，nc_controller新開一thread執行
	///
	///實作可選擇呼叫nc_controller.cmdProcress
	///cmdProcress會依序呼叫以下功能
	///1. _nc_controller_protocol.set_nc_controller_input
	///2. 依照步驟一對於client[index].bufferPtr，檢查封包是否接收完畢
	///3. 封包接收完畢後，依據步驟1的receiveHeader實作，呼叫對應的nc_controller介面
	///3-1. receiverHeader=0 or 1 呼叫AV_data_handling
	///3-2. receiverHeader=2 or d 呼叫
	///3-3. receiverHeader=e 呼叫event_packet_handling
	///3-4. receiverHeader=f 呼叫sdp_accept
	///4. 處理完畢後會呼叫txResp
	///
	///若選擇不使用nc_controller.cmdProcress的功能
	///可不用實作set_nc_controller_input，但需自行實作protocol處理函數
	///依照protocol命令呼叫對應的nc_controller介面AV_data_handling, event_packet_handling和sdp_accept
	void * (*entry_point)(void *);
	///protocol short ack，接收來自ncclient的訊息後，ack ncclient
	void (*txResp)(int index,int type);
	///event_packet_handling會呼叫get_event_info，取得Event_Keyword, Event_StatusTime和Event_MovieStartTime
	///get_event_info依照protocol的內容，指定對應的Event_Keyword常數，Event_Keyword參考Event_Keyword_XXX
	time_t (*get_event_info)(int type, void *event_packet_start);
	///event_packet_handling會呼叫check_start_recording，檢查是否可以觸發錄影
	int (*check_start_recording)(void *event_packet_start);
	///\brief ipcam命令功能，cam回傳的訊息處理
	///
	///依照各協定的定義，實作處理方式<br>
	///ms8101協定的處理參考nc_ms8101.h cam_resp_ms8101函數
	int (*cam_resp)(void *event_packet_start, int channel);
	///event_packet_handling處理初期執行
	int (*check_packet_format)(void *event_packet_start, int packetlen);
	///\brief 讀取該protocol專屬的設定
	///
	///實作需求
	///1. 可自訂一資料結構，將讀到的結果存入該結構，將_nc_controller_input的config_ptr指向該結構
	///2. 設定_nc_controller_input結構的protocol_type, protocol和targetaddress
	int (*read_config_file)(char *filepath);
	///nc系統初始化時，載入protocol模組系統函式
	int (*system_init)(struct _nc_controller_protocol *self);
	///nc系統結束時，卸載protocol模組系統函式
	int (*system_free)();
	///server type protocol使用的listen fd，不要直接初始化，請呼叫_nc_controller.init_network_fd
	///將定義變數名稱為_listen_fd
	struct _nc_controller_variable listen_fd;
	///自訂變數最多定義10組
	struct _nc_controller_variable_metadata variable[10];
	///自訂變數的數目，每當client呼叫define_variable函數中加1
	int variable_count;
	///server type protocol使用的listen function
	///收到封包後，呼叫find_idle_channel，取得一閒置的channel處理
	///接著create thread執行ncclient_session_main
	int (*listen_packet)(struct _nc_controller_protocol *self);
	///server listen
	struct ListenV6 serverV6;
	
	/*command module
	_Protocol_Command *
	toipcam_request_command_handling
	toipcam_ack_command_handling
	send_cam_command_handling
	cam_resp_command_handling
	需再自行實作socket IO得到封包的cam channel和封包長度 封包指標即可使用此API
	*/
	struct _Protocol_Command *command;
	long command_count;
	int (*handle_toipcam_request)(struct _nc_controller_protocol *protocol, void *camcom,u_int16_t ulen, int channel);
	int (*handle_toipcam_ack)(struct _nc_controller_protocol *protocol, void *camcom,u_int16_t ulen, char *msg, int channel);
	int (*handle_send_cam)(struct _nc_controller_protocol *protocol, void *camcom,u_int16_t ulen, int channel);
	int (*handle_cam_resp)(struct _nc_controller_protocol*, char *id, int channel);
	int (*get_protocol_command_type)(struct _nc_controller_protocol*, void *camcom);
	char* (*get_protocol_command_sender)(void *camcom);
	int command_buf_size;
	///protocol名字
	char name[28];
};

///ncclient到nc的介面，定義nc如何取得ncclient的資料
struct _nc_controller_input
{
	///server, client
	int protocol_type;
	///protocol類型ms8101, rtsp, onvif
	int protocol;
	///address and port
	struct sockaddr_in targetaddress;
	///read_config_file讀到的資料，設定到config_ptr
	char *config_ptr;
	///server type protocol使用的accept fd，不要直接初始化，請呼叫_nc_controller.init_network_fd
	///將定義變數名稱為_accept_fd
	struct _nc_controller_variable accept_fd;
	///自訂變數最多定義10組，使用get_variable取得自訂變數
	struct _nc_controller_variable variable[10];
	
	int channel;
	///依據protocol，指向對應的_nc_controller_protocol實作
	struct _nc_controller_protocol *controller_protocol;
};

//get_event_info實作使用
///當千里眼服務平台收到[event]SYN，會以[攝影機 command]htons(0)回覆。當攝影機收到[攝影機 command] htons(0)不須回覆[event]ACK
#define Event_Keyword_SYN 0
///用以載明偵測 器的事件觸發訊息，全時錄影會以此訊息傳送
#define Event_Keyword_CAM_EVENT 1
///全時錄影 video/ audio 回補的傳送 訊息
#define Event_Keyword_CAM_RSDVD 2
///開始全時連續錄影前，nc開新檔的訊息
#define Event_Keyword_CAM_SAVE1 3
#define Event_Keyword_CAM_IPCAM1 4
#define Event_Keyword_CAM_DVR_MV 5
///事件錄影
#define Event_Keyword_CAM_VIDEO 6
///即時影像的略圖訊息(JPEG 格式)
#define Event_Keyword_CAM_JPGLV 7
///事件發生時的影像略圖訊息(JPEG 格式)
#define Event_Keyword_CAM_JPGEV 8
///ipcam功能中，處理cam response
#define Event_Keyword_CAM_COMMD 9

///取得event命令編號
#define _Event_Keyword        0
///取得event命令發送時間epoch time
#define _Event_StatusTime     1
///取得event命令第一個frame的epoch time
#define _Event_MovieStartTime 2

//nc_controller api
///\brief 設定檔名，開啟檔案，準備開始錄影
///\param index camera channel
///\param timeNow value from time(&timeNow)
///\param epochLong_StatusTime camera發出此訊息時的epochLongTime ref to ms8101
///\param epochLong 第一個frame的epochLongTime ref to ms8101
///\param isRSDVD 全時錄影 video/ audio 回補的傳送 訊息，通常設為1
int begin_record(int index, time_t timeNow, time_t epochLong_StatusTime, time_t epochLong, char isRSDVD);

///\brief 開檔寫入audio格式擋頭
///\param index camera channel
///\param type 1全時錄影 2事件錄影
void createFileAudio(int index,int type);

///\brief 開檔寫入video格式擋頭
///\param index camera channel
///\param type 1全時錄影 2事件錄影
///\param sptr 檔案路徑
void createFile(int index,char *sptr,int type);

///\brief 停止錄影, 關檔將檔案搬到ok
///\param index camera channel
///\param type 1全時錄影 2事件錄影
///\param del 1刪除檔案
void moveFileAudio(int index,int type, int del);

///\brief 停止錄影, 關檔將檔案搬到ok
///\param index camera channel
///\param type 1全時錄影 2事件錄影
///\param timeNow 無用途
void moveFile(int index,time_t timeNow,int type);

///\brief 處理自ncclient傳來的封包
///
///1. 函數會判斷封包是否接收完畢，判斷的條件是
///client[index].bufferPtr - client[index].packetStart < client[index].packetLength
///封包的尾端減去封包起始點的差，是否和封包頭定義的封包長度相同
///2. 函數呼叫_nc_controller_input.set_nc_controller_input
///依照set_nc_controller_input實作的內容，判斷client.receiveHeader分別呼叫對應的處理函數
///  0x0 呼叫AV_data_handling
///  0x1 呼叫AV_data_handling
///  0x2 ?
///  0xf 呼叫sdp_accept
///  0xe 呼叫event_packet_handling
///  0xd 
int cmdProcress(int index);

///\brief 處理影音串流
///
///若由cmdProcress進入，當client.receiveHeader為0x0或0x1時，將呼叫此函數
///1. 檢查sdp是否已經收到
///2. 將串流資料依照codec的定義寫到檔案中
///3. 串流資料導向到relay server
int AV_data_handling(const int index, const int packetlen);

///\brief 處理event命令
///
///若由cmdProcress進入，當client.receiveHeader為0xe時，將呼叫此函數
///1. 檢查sdp是否已經收到
///2. 處理以下命令
///  2-1. 開關檔案
///  2-2. 事件錄影將串流寫入檔案
///  2-3. 縮圖
///  2-4. ipcam的cam_resp命令
int event_packet_handling(const int index, const int packetlen, void *pevent_packet);

///\brief 讀取sdp，並將sdp送到relay server
///
///若由cmdProcress進入，當client.receiveHeader為0xf時，將呼叫此函數
///1. 讀取sdp，取得video和audio編碼的資訊，設定codec的編碼
///2. 將sdp送到relay server，取得串流傳到relayserver的port
///3. 呼叫check_whilelist_cam
///新版本呼叫後內部會設定client[index].sdp=NewSDP;client[index].sdpOK=YES;client[index].sdp2relay=YES;
///\return Always return MS8101_OK
///\param index channel號碼
///\param sdpData sdp資料
///\param sdpLen sdp資料長度in byte
int sdp_accept(int index,unsigned char *sdpData,int sdpLen);

///\brief threadclient結束後執行，將變數reset為初始狀態
void reInitVar(int index);

///\brief 檢查cam是否可存取nc資源
///
///當nc.conf的WHITE_LIST_ENABLE=1時
///此功能將會啟動，程式連接127.0.0.1:4321
///檢查該index所屬cam的camid是否在白名單中
///\return 當cam通過檢查回傳MS8101_OK，否則回傳MS8101_NOK
int check_whilelist_cam(int index, time_t timeNow);

///\brief 將event command寫到eventtodo檔案，稍待寫入event server
void send_eventmsg_to_queue(const char *event_packet, const int packetlen, const time_t timeNow);

char *get_client_camid(const int channel);

int set_client_videoState(const int channel, const int val);
int get_client_videoState(const int channel);
int set_client_stopFlag(const int channel, const int val);
int get_client_rtpForward(const int channel);

u_int8_t *get_client_camCommand(const int channel);
int get_client_camCommandLen(const int channel);
int set_client_camCommandLen(const int channel, const int val);
int get_client_camCommandState(const int channel);
int set_client_camCommandState(const int channel, const int val);
int get_client_camCommandFd(const int channel);
int set_client_camCommandFd(const int channel, const int val);

int get_client_streamingserver_videosockfd(const int channel, const int streaming_serveri);
int reset_client_streamingserver_videosockfd(const int channel, const int streaming_serveri);

int get_client_acceptFd(const int channel);

int get_allForward();

///定義ncclient的自訂session變數，最多定義10組
int define_variable(struct _nc_controller_protocol *protocol, char *name, int size);
///取得變數名稱name的ncclient自訂session變數
int get_variable(int channel, char *name, long *ptr);

///初始化_nc_controller_input的network_fd
int init_accept_fd(int channel, int size, char *value);
int free_accept_fd(int channel);
int init_listen_fd(struct _nc_controller_protocol *protocol, int size, long *value);
int free_listen_fd(struct _nc_controller_protocol *protocol);

void *ncclient_session_main(void *channel);

void logMessage(int type,int index,time_t timeNow);
void closeallfd(int i);
///\brief 取得閒置的的channel，並且將本session註冊到該channel中
///
///註冊流程:
///1. 將session的protocol指定到channel
///2. 初始化session變數
int find_idle_channel(struct _nc_controller_protocol *protocol);

//ipcam api
int handle_standard_command(void *camcom,u_int16_t ulen, char *msg, int channel);
int handle_maintain_command(struct _nc_controller_protocol *protocol, void *camcom,u_int16_t ulen, char *msg);
int handle_cam_resp(struct _nc_controller_protocol *protocol, char *id, int channel);
struct _Protocol_Command * get_protocol_command(int channel);
#endif