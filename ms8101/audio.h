#ifndef _AUDIO_H_
#define _AUDIO_H_
///\addtogroup avapi
///@{
	
///audio codec編碼代號
#define AMR  0x00
#define PCMU 0x01
#define AAC  0x02
#define MAX_AUDIO_CODEC 0x2
///@}

#define AUDIO_OK  1
#define AUDIO_NOK 0
///\addtogroup avapi
///@{
	
typedef unsigned char AudioU8;
///\brief 依照輸入的fileext，呼叫對應的AudioFileHeaderWrite實作
///
///寫入audio檔案的檔頭
///\param audio_file input，欲寫入檔案的FILE結構指標
///\param audio_config input，AudioSDPAccept回傳的audio_config
///\param audio_config_len input，AudioSDPAccept回傳的audio_config_len
///\param fileext input，audio codec編碼代號
void AudioFileHeaderWriteHandling(FILE *audio_file, const char *audio_config, const int audio_config_len, const int fileext);
///\brief 依照輸入的fileext，呼叫對應的AudioFrameWrite實作
///
///寫入audio串流到檔案
///\param fp_aac input，欲寫入檔案的FILE結構指標
///\param pkt input，指向串流的指標
///\param len input，串流的長度	
///\param audio_config input，AudioSDPAccept回傳的audio_config
///\param fileext input，audio codec編碼代號
void AudioFrameWriteHandling(FILE *fp_aac,char *pkt, int len,const char *audio_config, const int fileext);
///\brief 解譯SDP audio sdp_track
///
///使用AudioHandling handlers逐筆檢查sdp中的codec名稱<br>
///找到對應的codec實作後，再呼叫對應的AudioSDPAccept
///\param fileExt output，依據sdp內容，指定audio的格式
///\param audio_config output，依據sdp內容，取得audio_config，供AudioFileHeaderWrite和AudioFrameWrite兩個功能使用
///\param audio_config_len output，audio_config的長度(?bytes)
///\param sdp_track input sdp的audio track字串
void AudioSDPAcceptHandling(AudioU8* fileExt, char *audio_config, int *audio_config_len, char *sdp_track);
///\brief 依照輸入的fileext，取得對應的附檔名名稱
///
///取得對應的附檔名名稱
///\param fileext input，audio codec編碼代號
char* AudioFileExtHandling(int fileext);
///\brief 依照輸入的fileext，檢查audio codec編碼代號是否合法
///
///檢查audio codec編碼代號是否合法
///\param fileext input，audio codec編碼代號
int AssertAudioCodecType(const int fileext);
///@}
#endif