#ifndef _VIDEO_H_
#define _VIDEO_H_
///\addtogroup avapi
///@{
	
///video codec編碼代號
#define M4V  0x00
#define H264 0x01
#define MAX_VIDEO_CODEC H264
///@}
#define VIDEO_OK  1
#define VIDEO_NOK 0
///\defgroup avapi AV API
///nc AV api<br>

///@{
typedef unsigned char VideoU8;
///\brief 依照輸入的fileext，取得對應的附檔名名稱
///
///取得對應的附檔名名稱
///\param fileext input，video codec編碼代號
char* VideoFileExtHandling(int fileext);
///\brief 解譯SDP video sdp_track
///
///使用VideoHandling video_handlers逐筆檢查sdp中的codec名稱<br>
///找到對應的codec實作後，再呼叫對應的VideoSDPAccept
///\param fileExt output，依據sdp內容，指定video的格式
///\param video_config output，依據sdp內容，取得video_config，供VideoFileHeaderWrite和VideoFrameWrite兩個功能使用
///\param video_config_len output，video_config的長度(?bytes)
///\param sdp_video_track input sdp的video track字串
int VideoSDPAcceptHandling(VideoU8* fileExt, VideoU8 *video_config, int *video_config_len, char *sdp_video_track);
///\brief 依照輸入的fileext，呼叫對應的VideoFrameWrite實作
///
///寫入video串流到檔案
///\param fileext video codec編碼代號
///\param fp 要被寫入串流資料的檔案fd
///\param pkt 串流資料
///\param len 串流資料長度
///\param video_config 由VideoSDPAcceptHandling取得之video_config，自sdp取得的相關串流資訊，將會被運算後寫入檔案
///\param WaitIFramEnable 設定是否要等待第一個frame為IFrame才寫入
///\param IsFirstPacket client.firstPacket
///\param IsIframe VideoCheckIFrameHandling的回傳值，檢查該frame是否為iframe
///\param WaitI client.wait264I
int VideoFrameWriteHandling(const int fileext, FILE *fp,char *pkt, int len,const VideoU8 *video_config, const VideoU8 WaitIFramEnable, const VideoU8 IsFirstPacket, const VideoU8 IsIframe, VideoU8 *WaitI);
///\brief 依照fileext，呼叫對應的VideoCheckIFrame
///
///檢查frame pkt是否為iframe
///\param fileext video codec編碼代號
///\param pkt 串流資料
int VideoCheckIFrameHandling(char *pkt,int fileext);
///\brief 依照輸入的fileext，呼叫對應的VideoFileHeaderWrite實作
///
///寫入audio檔案的檔頭
///\param video_file input，欲寫入檔案的FILE結構指標
///\param video_config input，AudioSDPAccept回傳的video_config
///\param video_config_len input，AudioSDPAccept回傳的video_config_len
///\param fileext input，video codec編碼代號
int VideoFileHeaderWriteHandling(const int fileext, FILE *video_file, const char *video_config, const int video_config_len);
///@}
#endif