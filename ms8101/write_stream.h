///\addtogroup avapi
///@{
	
///\brief 處理RTP串流的介面
///
///處理由DVR傳來的RTP串流，RTP串流由client[index].packetStart取得<br>
///呼叫此介面前，若該RTP串流為video串流，需先檢查是否為Iframe，若為Iframe的話，IsIframe設為1<br>
///packetlen是RTP串流的資料長度
typedef int (*write_stream_interface)(const int index, const int IsIframe, const int packetlen);

///\brief把串流寫到/disk1/recording
int write_rtp_stream_to_record(const int index, const int IsIframe, const int packetlen);
///\brief 將串流資料寫到ringbuffer中，供事件錄影用
int save2Buffer(const int index, const int IsIframe, const int packetlen);
///\brief 串流傳到darwin 
int forward_stream_to_darwin(const int index, const int IsIframe, const int packetlen);
///\brief AV_data_handling呼叫的實作
write_stream_interface write_stream_actions[]=
{
	write_rtp_stream_to_record,
	save2Buffer,
	forward_stream_to_darwin
};
///@}
#define write_stream_actions_count 3