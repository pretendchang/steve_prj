#include "main.h"
#ifndef _NC_CONTROLLER_H_
#define _NC_CONTROLLER_H_
#define CONTROLLER_NOK 0
#define CONTROLLER_OK 1
#define CONTROLLER_ERROR -1
#define LISTEN_PACKET_RETRY 2
#define LISTEN_PACKET_BUSY  3
#define FIND_NO_MATCHED_MAINTAINCOMMAND -1

#define Protocol_Plugin __attribute__((section(".protocol_module")))
///\defgroup commandapi Command API
///nc command api<br>
///260<br>
///\startuml
///	人 -> CGI: 看即時影像
///	CGI -> ipcam: send 260 command
///	ipcam -> nc_controller: handle_standard_command
///	nc_controller -> plugin: handle_toipcam_request
///	plugin -> protocol_command: toipcam_request
///	protocol_command -> nc_controller: call api
///	nc_controller -> plugin: handle_toipcam_ack
///	plugin -> protocol_command: toipcam_ack
///	ipcam -> CGI: ack camid
///	nc_controller -> plugin: handle_send_cam
///	plugin -> protocol_command: send_cam
///	nc_controller -> DVR: request
///	DVR -> nc_controller: resp(ms8101 header 0xe)
///	nc_controller -> plugin: handle_cam_resp
///	plugin -> protocol_command: cam_resp
///	protocol_command -> nc_controller: call api
///\enduml	
///\startuml
///nc_controller -> plugin: handle_toipcam_request
///plugin -> protocol_command: toipcam_request_handling
///protocol_command->protocol_command: FindCommandHandler
///protocol_command->protocol_command: toipcam_request_default
///protocol_command->protocol_command: toipcam_request_260
///protocol_command -> nc_controller: call api
///\enduml
///512
///\startuml
///	人 -> CGI: 查cam設定參數
///	CGI -> ipcam: send 512 command
///	ipcam -> nc_controller: handle_standard_command
///	nc_controller -> plugin: handle_toipcam_request
///	plugin -> protocol_command: toipcam_request
///	protocol_command -> nc_controller: call api
///	nc_controller -> plugin: handle_toipcam_ack
///	plugin -> protocol_command: toipcam_ack
///	ipcam -> CGI: ack camid
///	nc_controller -> plugin: handle_send_cam
///	plugin -> protocol_command: send_cam
///	nc_controller -> DVR: request
///	DVR -> nc_controller: resp(ms8101 header 0xd)
///	nc_controller -> CGI: send back result
///\enduml	
///\startuml
///nc_controller -> plugin: handle_toipcam_request
///plugin -> protocol_command: toipcam_request_handling
///protocol_command->protocol_command: FindCommandHandler
///protocol_command->protocol_command: toipcam_request_default
///\enduml
///65534
///\startuml
///	人 -> CGI: 看即時影像
///	CGI -> ipcam: send 512 command
///	ipcam -> nc_controller: handle_maintain_command
///	nc_controller -> maintain_command: maintain_toipcam_request_handling
///	maintain_command -> maintain_command: toipcam_request
///	maintain_command -> maintain_command: find_maintain_command_handler
///	maintain_command -> maintain_command: maintain_toipcam_request_default
///	maintain_command -> maintain_command: toipcam_request_65534
///	maintain_command -> nc_controller: call api
///	nc_controller -> maintain_command: handle_toipcam_ack
///	maintain_command -> maintain_command: toipcam_ack
///	maintain_command -> maintain_command: find_maintain_command_handler
///	maintain_command -> maintain_command: maintain_toipcam_ack_default
///	maintain_command -> CGI: ack camid
///\enduml	
///@{
	
///\brief Protocol_Command類型
///
///各類型command說明
///1. standard command用來監控個別攝影機，maintain command用來監控整個ncs服務的狀況
enum _Protocol_Command_Type {
	 STANDARD
	,IPCAM
	,MAINTAIN
};
///@}

///\defgroup variableapi Variable API
///nc variable api設定plugin config and session變數<br>
///範例程式<br>
///定義和設定值給config variable
///\snippet helloworld\helloworld.c define_set_variable
///session關連到config variable
///\snippet helloworld\helloworld.c link_variable
///取出config variable
///\snippet helloworld\helloworld.c get_variable
///define_config_variable
///\startuml
///	client -> config_variable: define_config_variable
///	config_variable -> variable: define_variable
///	variable -> variable: set metadata name, size and type
///	config_variable -> config_variable: malloc variable array
///	config_variable -> config_variable: malloc every variable array element ptr and points to metadata
///\enduml	
///set_config_variable
///\startuml
///	client -> config_variable: strcmp(name)
///	config_variable -> config_variable: memcpy(ptr, value)
///\enduml	
///link_config_variable_to_controller
///\startuml
///	client -> config_variable: points controller_input->config_variable to real memory location
///\enduml	
///@{
	
///\brief _nc_controller_variable類型
///
///SESSION variable在呼叫find_idle_session取得session的index後，才進行記憶體配置，用來儲存session執行期間的變數
///CONFIG variable呼叫define_config_variable時，即進行記憶體配置，用來儲存plugin設定參數
enum _VARIABLE_Type {
	 SESSION
	,CONFIG
};
///@}
///\addtogroup commandapi
///@{

///\brief 處理外部系統(ipcam_client)對nc的通訊
///
///前提：
///1. nc已收到該session攝影機的SDP\n
///主流程：
///1. ipcam_client送出稍後要送出的camcom_t長度(封包長度:2 byte) ms8101協定
///2. ipcam_client送出toipcam_request camcom_t
///3. nc.toipcam回應toipcam_ack to ipcam_client camid=camxxxxxxx
///4. nc在toipcam處理camcom_t封包
///5. nc.tcpin 送出send_cam camcom到ncclient
///6. ncclient送出cam_resp camcommand(in ms8101_t)
///7. nc在tcpin處理該cam_resp camcommand
///8. nc把camcommand dequeue到event queue
///9. 若步驟五ack是0xd config，nc會把此封包再傳到toipcam_client
///10. 若有執行步驟9 toipcam_client會再ack訊息給nc，由步驟1開始ack訊息的流程到步驟8<p>
///實作_Protocol_Command新增ipcam功能 SOP:
///1. 程式碼需include nc_controller.h
///2. 定義實作新的_Protocol_Command物件
///3. 將該物件沒有實作的function則定義NULL，handler檢查函式指標為NULL時，僅會執行預設的行為
///4. 將該物件加入_Protocol_Command array中
///ms8101範例
///\snippet ms8101\ms8101_command_handlers.h ms8101_command_handlers_label
struct _Protocol_Command
{
	enum _Protocol_Command_Type type;
	///_Protocol_Command訊息id，ms8101協定中，commandid用在toipcam_request toipcam_ack和send_cam
	int commandid;
	///_Protocol_Command訊息名稱，ms8101協定中用在cam_resp
	char cam_comm[10];
	
	///\brief 接收處理處理由ipcam client送來的camcom封包
	///
	///\param camcom ipcam client送來的camcom封包
	///\param ulen ipcam client送來的camcom封包的長度，? byte
	///\param session_idx ipcam client送來的封包送到哪個攝影機處理，該攝影機所在的session index
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK
	int (*toipcam_request)(void *camcom,u_int16_t ulen, int session_idx);
	
	///\brief 接收到由ipcam client送來的camcom封包後，回傳訊息給ipcam client，通知處理結果
	///
	///系統預設回傳無data資料
	///若要修改回傳的內容，修改msg，實作toipcam_ack_xxx並加到_Protocol_Command array(ms8101_command_handlers)中
	///\param camcom ipcam client送來的camcom封包
	///\param ulen ipcam client送來的camcom封包的長度，? byte
	///\param msg 回傳給ipcam client的訊息
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK	
	int (*toipcam_ack)(void *camcom, u_int16_t ulen, char *msg);
	
	///\brief 接收到由ipcam client送來的camcom命令後，nc將此命令發送到nc_client，通知nc client執行相關動作
	///
	///系統預設回傳不帶data的camCommand_t結構\n
	///若要增加data欄位的內容，需實作send_cam_xxx並加到_Protocol_Command array(ms8101_command_handlers)中
	///\param camcom_t 送到nc_client的送camcom封包
	///\param ulen 送到nc_client的送camcom_t封包的長度，? byte
	///\param session_idx nc_client的session index
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK
	int (*send_cam)(void *camcom, u_int16_t ulen, int session_idx);
	
	///\brief nc_client接收到send_cam命令後，回傳給nc的資料封包處理函式
	///
	///\param session_idx nc_client的session index
	///\return 處理成功回傳MS8101_OK，否則回傳MS8101_NOK
	int (*cam_resp)(int session_idx);
};
///@}
///\addtogroup variableapi
///@{
	
///每個protocol可設定的session variable上限為10
#define MAX_SESSION_VARIABLE 10
///每個protocol可設定的config variable上限為10
#define MAX_CONFIG_VARIABLE 10
///每個protocol可設定的variable上限為20(session variable+config variable)
#define MAX_VARIABLE MAX_SESSION_VARIABLE+MAX_CONFIG_VARIABLE
///\brief nc_controller 變數的metadata
///
///metadata定義變數的名稱、變數類型和占用的記憶體大小<br>
///變數類型分為session和config兩類<br>
///session變數用來儲存session執行期間的變數<br>
///當session變數呼叫define_session_variable函數以特定名字定義後，名字將儲存在name欄位<br>
///取用時呼叫get_session_variable以名字來取得session變數指標<br>
///size定義變數占用的記憶體大小，當nc收到ncclient連線後，呼叫find_idle_session時，nc使用malloc配置大小為size的記憶體空間<br>
///因此get_session_variable需在controller呼叫find_idle_session後，session建立時，才可呼叫，否則get_session_variable會回傳尚未初始化的錯誤<br>
///當該ncclient連線的session結束後，nc會呼叫free，釋放記憶體空間<p><br>
///config變數用來儲存plugin的設定值<br>
///呼叫define_config_variable時，nc使用malloc配置大小為size的記憶體空間，因此可用在read_config中使用set_config_variable儲存參數<br>
///session建立後可呼叫link_config_variable_to_controller將變數和session做關聯<br>
///當plugin進行free時，config variable的空間會被釋放
struct _nc_controller_variable_metadata
{
	///_nc_controller_variable的名字
	char name[16];
	///_nc_controller_variable的型態
	enum _VARIABLE_Type variable_type;
	///_nc_controller_variable的ptr空間大小
	int  size;
};

///\brief _nc_controller變數
///
///提供plugin定義變數，metadata定義變數的規格，ptr指向存放變數的記憶體位置
struct _nc_controller_variable
{
	///指向metadata，描述變數的規格
	struct _nc_controller_variable_metadata *metadata;
	///指向存放變數資料的記憶體位置
	void *ptr;
};
///@}
///\defgroup plugin Plugin API
///nc protocol plugin api<br>	
///nc 啟動後循序圖<br>
///\startuml
///	人 -> nc_controller: 開機
///	nc_controller -> nc_controller : load_protocols
///	nc_controller -> nc_controller : argsRead
///	activate nc_controller
///	nc_controller -> plugin1: argsRead_plugin1
///	deactivate nc_controller
///	nc_controller -> nc_controller : read_config_file
///	activate nc_controller
///	nc_controller -> plugin1: read_config_file_plugin1
///	deactivate nc_controller
///	nc_controller -> nc_controller : init_system
///	activate nc_controller
///	nc_controller -> plugin1: system_init_plugin1
///	deactivate nc_controller
///	alt while(1)
///	nc_controller -> nc_controller : listen_packet
///	activate nc_controller
///	nc_controller -> nc_controller: find_idle_channel
///	nc_controller -> nc_controller: ncclient_session
///	nc_controller -> plugin1: entry_point_plugin1
///	deactivate nc_controller
///	end
///\enduml
///@{
	
///\brief ncclient到nc的通訊介面protocol格式
///
///nc_controller執行_nc_controller_protocol次序關係如下
///\startuml
///nc_controller -> nc_controller:nc_session_main
///group by camera session_index
///create thread
///nc_controller -> thread:pthread_create
///thread -> plugin_ms8101:entry_point_ms8101
///plugin_ms8101 -> plugin_ms8101:set_nc_controller_input
///alt video packet receiveHeader=0x0 or audio packet 0x1
///plugin_ms8101 -> AV:AV_data_handling
///activate AV
///alt Get an IFrame 
///AV -> plugin_ms8101 :txResp
///end
///deactivate AV
///else time packet receiveHeader=0xc
///plugin_ms8101 -> plugin_ms8101:txResp
///else config packet receiveHeader=0xd
///plugin_ms8101 -> plugin_ms8101:txResp
///plugin_ms8101 -> plugin_ms8101:cam_resp
///else event packet receiveHeader=0xe
///plugin_ms8101 -> event:event_packet_handling
///activate event
///event -> plugin_ms8101:check_packet_format
///event -> plugin_ms8101:get_event_info
///alt SYN
///event -> plugin_ms8101:txResp
///end
///event -> eventtodo:send_eventmsg_to_queue
///alt CAM_EVENT
///event -> plugin_ms8101:txResp
///else 全時錄影或事件錄影
///event -> plugin_ms8101:check_start_recording
///event -> plugin_ms8101:txResp
///else CAM_JPGLV
///event -> plugin_ms8101:txResp
///else CAM_JPGEV
///event -> plugin_ms8101:txResp
///else CAM_COMMD
///event -> plugin_ms8101:cam_resp
///end
///deactivate event
///else sdp packet receiveHeader=0xf
///plugin_ms8101 -> sdp:sdp_accept
///activate sdp
///sdp -> plugin_ms8101:txResp
///deactivate sdp
///else picture packet receiveHeader=0x2
///end
///end
///nc_controller -> nc_controller:system_free
///\enduml


///\startuml
///nc_controller -> nc_controller:read_config_file
///activate nc_controller
///nc_controller -> plugin_rtsp:read_config_file_rtsp
///deactivate nc_controller
///activate plugin_rtsp
///plugin_rtsp -> nc_controller:define_config_variable
///plugin_rtsp -> nc_controller:set_config_variable
///deactivate plugin_rtsp
///nc_controller -> nc_controller:init_system
///activate nc_controller
///nc_controller -> plugin_rtsp:system_init_rtsp
///deactivate nc_controller
///activate plugin_rtsp
///plugin_rtsp -> nc_controller:find_idle_session
///plugin_rtsp -> nc_controller:link_config_variable_to_controller
///plugin_rtsp -> nc_controller:nc_session_main
///create thread
///plugin_rtsp -> thread:pthread_create
///deactivate plugin_rtsp
///thread -> plugin_rtsp:entry_point_rtsp
///nc_controller -> nc_controller:system_free
///\enduml

///\verbatim
///實作SOP
///1. 實作_nc_controller_protocol結構所有函數
///2. 宣告步驟1結構為Protocol_Plugin，指定Plugin的初始值
///nc_controller架構
///Packet header + payload
///Packet header = receive_header + payload length
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
	///1. payload receive_header設定在client[session_idx].receiveHeader
	///2. 檢查封包的標頭是否接收完畢
	///3. packet_length設定在client[session_idx].packetLength(in byte)
	///4. 設定client[session_idx].packetStart在payload第一個byte
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
	///\snippet ms8101\nc_ms8101.c set_nc_controller_input_ms8101_label
	int (*set_nc_controller_input)(const int session_idx, const void *packet);
	///\brief protocol處理函數的進入點，nc_controller新開一thread執行ncclient_session_main後，會呼叫此函數
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
	int (*cam_resp)(void *event_packet_start, int session_idx);
	///event_packet_handling處理初期執行
	int (*check_packet_format)(void *event_packet_start, int packetlen);
	///\brief 讀取該protocol專屬的設定
	///
	///實作需求
	///1. 可自訂一資料結構，將讀到的結果存入該結構，將_nc_controller_input的config_ptr指向該結構
	///2. 設定_nc_controller_input結構的protocol_type, protocol和targetaddress
	int (*read_config_file)(struct _nc_controller_protocol *self, char *filepath);
	///nc系統初始化時，載入protocol模組系統函式
	int (*system_init)(struct _nc_controller_protocol *self);
	///nc系統結束時，卸載protocol模組系統函式
	int (*system_free)();
	///server type protocol使用的listen fd，不要直接初始化，請呼叫_nc_controller.init_network_fd
	///將定義變數名稱為_listen_fd
	struct _nc_controller_variable listen_fd;

	///自訂變數(session+config)
	///\ingroup variableapi
	struct _nc_controller_variable_metadata variable[MAX_VARIABLE];
	///自訂config變數
	///\ingroup variableapi
	struct _nc_controller_variable *config_variable[MAX_CONFIG_VARIABLE];
	///自訂變數(variable)的數目，每當client呼叫define_variable函數中加1
	///\ingroup variableapi
	int variable_count;
	///自訂config變數(config_variable)，每組變數的數目，呼叫define_config_variable函數時，variable_element_count的值
	///\ingroup variableapi
	int config_variable_count[MAX_CONFIG_VARIABLE];

	///server type protocol使用的listen function
	///收到封包後，呼叫find_idle_session，取得一閒置的session處理
	///接著create thread執行ncclient_session_main
	///\return 0:錯誤 1:成功 2:進行下一輪 3:找不到閒置的session
	int (*listen_packet)(struct _nc_controller_protocol *self);
	///處理執行參數
	int (*read_args)(char opt, char *optarg);
	///執行參數使用說明
	int (*args_usage)();
	///註冊protocol的執行參數(optarg)到nc_controller
	int (*register_protocol)();
	///server type protocol的socket參數
	///ip port皆存放於此
	struct ListenV6 serverV6;
	
	///指向_Protocol_Command的實作
	///\ingroup commandapi
	struct _Protocol_Command *command;
	///_Protocol_Command指向的資料大小
	///\ingroup commandapi
	long command_count;
	///command模組toipcam_request handling，分派toipcam_request到對應的_Protocol_Command實作
	///\ingroup commandapi
	int (*handle_toipcam_request)(struct _nc_controller_protocol *protocol, void *camcom,u_int16_t ulen, int session_idx);
	///command模組toipcam_ack handling，分派toipcam_ack到對應的_Protocol_Command實作
	///\ingroup commandapi
	int (*handle_toipcam_ack)(struct _nc_controller_protocol *protocol, void *camcom,u_int16_t ulen, char *msg, int session_idx);
	///command模組send_cam handling，分派send_cam到對應的_Protocol_Command實作
	///\ingroup commandapi
	int (*handle_send_cam)(struct _nc_controller_protocol *protocol, void *camcom,u_int16_t ulen, int session_idx);
	///command模組cam_resp handling，分派cam_resp到對應的_Protocol_Command實作
	///\ingroup commandapi
	int (*handle_cam_resp)(struct _nc_controller_protocol*, char *id, int session_idx);
	///回傳camcom封包的command型式：_Protocol_Command_Type
	///\ingroup commandapi
	int (*get_protocol_command_type)(struct _nc_controller_protocol*, void *camcom);
	///取得ipcam module傳來的camcom封包，要傳送的目標cam，該cam的id
	///\param camcom ipcam module傳來的封包，封包格式由plugin自訂
	///\ingroup commandapi
	char* (*get_protocol_command_sender)(void *camcom);
	///\ingroup commandapi
	int (*find_command)(struct _nc_controller_protocol *protocol, int id);
	///command模組中，camcom的空間大小
	///\ingroup commandapi
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
	///\ingroup variableapi
	struct _nc_controller_variable variable[MAX_SESSION_VARIABLE];
	///設定變數最多定義10組，使用get_variable取得設定變數
	///\ingroup variableapi
	struct _nc_controller_variable *config_variable[MAX_CONFIG_VARIABLE];
	///\ingroup variableapi
	int config_variable_count;
	
	int session_idx;
	///依據protocol，指向對應的_nc_controller_protocol實作
	struct _nc_controller_protocol *controller_protocol;
};
//txResp實作使用
#define TYPE_ACK_I         0x0000
#define TYPE_ACK_SDP       0x0001
#define TYPE_ACK_SYN		  0x0002
#define TYPE_ACK_EVENT     0x0003
#define TYPE_ACK_VSTART    0x0004
#define TYPE_ACK_VSTOP     0x0005
#define TYPE_ACK_CONFIG    0x0007
#define TYPE_ACK_TIME      0x0008
#define TYPE_ACK_SSTART    0x0009
#define TYPE_ACK_SSTOP     0x000a
#define TYPE_ACK_RSTART    0x000d
#define TYPE_ACK_RSTOP     0x000e
#define TYPE_ACK_JLVSTART  0x0018
#define TYPE_ACK_JLVSTOP   0x0019
#define TYPE_ACK_JEVSTART  0x001a
#define TYPE_ACK_JEVSTOP   0x001b

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

//nc_controller_action api
///\brief 設定檔名，開啟檔案，準備開始錄影
///\param session_idx camera session index
///\param timeNow value from time(&timeNow)
///\param epochLong_StatusTime camera發出此訊息時的epochLongTime ref to ms8101
///\param epochLong 第一個frame的epochLongTime ref to ms8101
///\param isRSDVD 全時錄影 video/ audio 回補的傳送 訊息，通常設為1
int begin_record(int session_idx, time_t timeNow, time_t epochLong_StatusTime, time_t epochLong, char isRSDVD);

///\brief 開檔寫入audio格式擋頭
///\param session_idx camera session index
///\param type 1全時錄影 2事件錄影
void createFileAudio(int session_idx,int type);

///\brief 開檔寫入video格式擋頭
///\param session_idx camera session index
///\param type 1全時錄影 2事件錄影
///\param sptr 檔案路徑
void createFile(int session_idx,char *sptr,int type);

///\brief 停止錄影, 關檔將檔案搬到ok
///\param session_idx camera session index
///\param type 1全時錄影 2事件錄影
///\param del 1刪除檔案
void moveFileAudio(int session_idx,int type, int del);

///\brief 停止錄影, 關檔將檔案搬到ok
///\param session_idx camera session index
///\param type 1全時錄影 2事件錄影
///\param timeNow 無用途
void moveFile(int session_idx,time_t timeNow,int type);

///\brief 處理自ncclient傳來的封包
///
///1. 函數會判斷封包是否接收完畢，判斷的條件是
///client[session_idx].bufferPtr - client[session_idx].packetStart < client[session_idx].packetLength
///封包的尾端減去封包起始點的差，是否和封包頭定義的封包長度相同
///2. 函數呼叫_nc_controller_input.set_nc_controller_input
///依照set_nc_controller_input實作的內容，判斷client.receiveHeader分別呼叫對應的處理函數
///  0x0 呼叫AV_data_handling
///  0x1 呼叫AV_data_handling
///  0x2 ?
///  0xf 呼叫sdp_accept
///  0xe 呼叫event_packet_handling
///  0xd 
int cmdProcress(int session_idx);

///\brief 處理影音串流
///
///若由cmdProcress進入，當client.receiveHeader為0x0或0x1時，將呼叫此函數
///1. 檢查sdp是否已經收到
///2. 將串流資料依照codec的定義寫到檔案中
///3. 串流資料導向到relay server
int AV_data_handling(const int session_idx, const int packetlen);

///\brief 處理event命令
///
///若由cmdProcress進入，當client.receiveHeader為0xe時，將呼叫此函數<br>
///函數會呼叫send_eventmsg_to_queue將pevent_packet資料傳到event server<br>
///1. 檢查sdp是否已經收到<br>
///2. 處理以下命令<br>
///  2-1. 開關檔案<br>
///  2-2. 事件錄影將串流寫入檔案<br>
///  2-3. 縮圖<br>
///  2-4. ipcam的cam_resp命令<br>
int event_packet_handling(const int session_idx, const int packetlen, void *pevent_packet);

///\brief 讀取sdp，並將sdp送到relay server
///
///若由cmdProcress進入，當client.receiveHeader為0xf時，將呼叫此函數
///1. 讀取sdp，取得video和audio編碼的資訊，設定codec的編碼
///2. 將sdp送到relay server，取得串流傳到relayserver的port
///3. 呼叫check_whilelist_cam
///新版本呼叫後內部會設定client[session_idx].sdp=NewSDP;client[session_idx].sdpOK=YES;client[session_idx].sdp2relay=YES;
///\return Always return MS8101_OK
///\param session_idx cam session index
///\param sdpData sdp資料
///\param sdpLen sdp資料長度in byte
int sdp_accept(int session_idx,unsigned char *sdpData,int sdpLen);

///\brief threadclient結束後執行，將變數reset為初始狀態
void reInitVar(int session_idx);

///\brief 檢查cam是否可存取nc資源
///
///當nc.conf的WHITE_LIST_ENABLE=1時
///此功能將會啟動，程式連接127.0.0.1:4321
///檢查該index所屬cam的camid是否在白名單中
///\return 當cam通過檢查回傳MS8101_OK，否則回傳MS8101_NOK
int check_whilelist_cam(int session_idx, time_t timeNow);

///\brief 將event command寫到eventtodo檔案，稍待寫入event server
///
///\param event_packet 傳到event server的內容
///\param packetlen event_packet的長度
///\param timeNow 寫到event server的timestamp(長度11 bytes)
void send_eventmsg_to_queue(const char *event_packet, const int packetlen, const time_t timeNow);

char *get_client_camid(const int session_idx);
struct _nc_controller_protocol* get_client_plugin(const int session_idx);

int set_client_videoState(const int session_idx, const int val);
int get_client_videoState(const int session_idx);
int set_client_stopFlag(const int session_idx, const int val);
int get_client_rtpForward(const int session_idx);
///@}
///\addtogroup commandapi
///@{
	
///\brief 取得camCommand指標
///	
///camCommand是ipcam thread收到command後，將command轉到session thread的共用記憶體區塊<br>
///command api透過toipcam_request收到camcom後，使用send_cam將camcom命令轉為camCommand<br>
///session thread再把camCommand送到DVR<br>
///為了達到ipcam thread和session thread兩個共用camCommand，使用camCommandState來控制存取該記憶體的同步<br>
///ipcam thread收到camcom命令轉為camCommand完成後，camCommandState設定為YES<br>
///session thread檢查camCommandState為YES時，將camCommand送到DVR，並把camCommandState更改為NO
///camCommandFd用來儲存ipcam client的socket，整個command程序進行到最後一流程cam_resp時<br>
///若DVR回傳的命令須再送到ipcam client(ex: ms8101 513)，即可透過此變數回傳
///camCommand指標可用於send_cam實作中
u_int8_t *get_client_camCommand(const int session_idx);
///取得camCommand指標指向的記憶體長度
int get_client_camCommandLen(const int session_idx);
///設定camCommand指標指向的記憶體長度
int set_client_camCommandLen(const int session_idx, const int val);
///\brief 取得camCommand的State
///
///參考get_client_camCommand說明
int get_client_camCommandState(const int session_idx);
///\brief 設定camCommand的State
///
///參考get_client_camCommand說明
int set_client_camCommandState(const int session_idx, const int val);
///\brief 取得camCommand的FD
///
///參考get_client_camCommand說明
int get_client_camCommandFd(const int session_idx);
///\brief 設定camCommand的FD
///
///參考get_client_camCommand說明
int set_client_camCommandFd(const int session_idx, const int val);
///\brief 處理standard command
///
///依序呼叫protocol plugin實作的handle_toipcam_request, handle_toipcam_ack和handle_send_cam<br>
///若plugin沒有實作handle_send_cam，則此command只會執行handle_toipcam_request, handle_toipcam_ack兩個程序<br>
///\param camcom ipcam收到由client傳來的封包，封包deserialize由各plugin自行實作，ms8101的格式為camcom_t
///\param ulen camcom封包的長度
///\param msg 初始化完畢的記憶體空間，用來儲存toipcam_ack命令回傳到ipcam client的封包
///\param session_idx 處理此封包的session index
int handle_standard_command(void *camcom,u_int16_t ulen, char *msg, int session_idx);
///\brief maintain command
///
///依序呼叫global maintain和protocol plugin實作的handle_toipcam_request, handle_toipcam_ack<br>
///\param command 處理camcom封包對應的_Protocol_Command實作
///\param camcom ipcam收到由client傳來的封包，封包deserialize由各plugin自行實作，ms8101的格式為camcom_t
///\param ulen camcom封包的長度
///\param msg 初始化完畢的記憶體空間，用來儲存toipcam_ack命令回傳到ipcam client的封包
int handle_maintain_command(struct _Protocol_Command *command, void *camcom,u_int16_t ulen, char *msg);
///處理dvr執行camCommand後回傳到nc的命令
int handle_cam_resp(struct _nc_controller_protocol *protocol, char *id, int session_idx);
///\brief 搜尋maintain command
///
///輸入camcom_t的command id搜尋對應的maintain command
///maintain command可為global command或是protocol command
struct _Protocol_Command*  find_command(int id);

///@}
///\addtogroup plugin
///@{
	
int get_client_streamingserver_videosockfd(const int session_idx, const int streaming_serveri);
int reset_client_streamingserver_videosockfd(const int session_idx, const int streaming_serveri);

int get_client_acceptFd(const int session_idx);

int get_allForward();

struct _streaming_server_info* get_streaming_server_info(int session_idx);
///@}
///\addtogroup variableapi
///@{
	
///\brief定義nc的自訂變數，包含SESSION和CONFIG最多定義20組
///
///初始化_nc_controller_variable的metadata
///\param protocol protocol plugin_name
///\param name 變數的名稱，將會設定在metadata的name欄位
///\param size 變數占用的記憶體空間大小(?bytes)，設定在meata的size欄位
///\param variable_type 變數的型態，SESSION或是CONFIG
int define_variable(struct _nc_controller_protocol *protocol, char *name, int size, enum _VARIABLE_Type variable_type);
///brief 取得session變數
///
///\param name session變數名稱
///\param ptr 存放session變數的空間，須預先配置
///\return: 0 找不到變數  1 ok  -1 變數尚未配置記憶體
int get_session_variable(int session_idx, char *name, uintptr_t *ptr);
///\brief 關聯session中的config變數到config變數陣列區塊的某個元素
///
///\param protocol plugin指標
///\param session_idx cam session index
///\param index session指向的config變數陣列的元素
///\param name session指向的config變數名稱
int link_config_variable_to_controller(struct _nc_controller_protocol *protocol, int session_idx, int index, char *name);
///設定config變數值，函數會將value指向的記憶體區塊內容，複製到config變數中
int set_config_variable(struct _nc_controller_protocol *protocol, int index, char *name, char *value);
///\brief定義nc的config變數陣列區塊，包含SESSION和CONFIG最多定義20組
///
///初始化config變數，儲存變數的記憶體空間在此函數初始化，底層呼叫define_variable初始化metadata
///\param protocol protocol plugin_name
///\param name 變數的名稱，將會設定在metadata的name欄位
///\param size 變數占用的記憶體空間大小(?bytes)，設定在meata的size欄位
///\param variable_element_count 陣列的大小
int define_config_variable(struct _nc_controller_protocol *protocol, char *name, int variable_size, int variable_element_count);
///\brief定義nc的session變數
///
///session變數儲存變數的記憶體空間，呼叫find_idle_session時才會初始化，底層呼叫define_variable初始化metadata
///\param protocol protocol plugin_name
///\param name 變數的名稱，將會設定在metadata的name欄位
///\param size 變數占用的記憶體空間大小(?bytes)，設定在meata的size欄位
int define_session_variable(struct _nc_controller_protocol *protocol, char *name, int size);
///取得config變數指標
int get_config_variable(int session_idx, char *name, uintptr_t *ptr);
///@}
///\addtogroup plugin
///@{
	
///初始化_nc_controller_input的network_fd
int init_accept_fd(int session_idx, int size, char *value);
int free_accept_fd(int session_idx);
int init_listen_fd(struct _nc_controller_protocol *protocol, int size, long *value);
int free_listen_fd(struct _nc_controller_protocol *protocol);
int* get_accept_fd(int session_idx);
void *ncclient_session_main(void *session_idx);
struct _nc_controller_protocol * hook_protocol_plugin(char *plugin_name);

void logMessage(int type,int index,time_t timeNow);
void closeallfd(int i);
///\brief 取得閒置的的session_idx，並且將本session註冊到該idx中
///
///註冊流程:
///1. 將session的protocol指定到session_idx
///2. 初始化session變數
int find_idle_session(struct _nc_controller_protocol *protocol);

///\brief 設定plugin的執行參數(optarg)到controller
///
///\param opt 參數字元
///\param hasoptarg 該參數是否有帶值 0:沒有 1:有
///\return -1:參數名稱衝突  1:成功
int register_opts(char opt, int hasoptarg);
///@}
#endif