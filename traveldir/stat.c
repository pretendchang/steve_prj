
#include "stat.h"

/*
用途(use case)
   -1. 巢狀處理樹狀結構，使用scandir掃描出本階層符合filter條件的枝幹，並依照compar邏輯做排序
   -2. 每個枝幹會呼叫枝幹處理Handling
   -3. 每個枝幹處理前會依照breakcondition檢查，若不需繼續輪巡所有枝幹，則提前結束本階層的Handling
架構
   traveldir為controller，程式的核心
   利用此核心需實作以下function
   -1. Handling
       處理每個枝幹的工作
   -2. breakcondition
       輪巡枝幹中斷的判斷函數，若判斷成立可提前結束此層枝幹的輪巡
   -3. filter
      使用於scandir，檢查傳入的檔名是否符合命名規則
   -4. compar
      使用於scandir，排序檔案的邏輯
   
目前實作
   -1. 應用於目錄結構  /cam/date/file 的掃瞄
   -2. 若磁碟容量將滿，則每個cam刪除一個最舊的file
   -3. 刪除過期的file，date和file會依照時間排序，當過其檔案刪完，則提前結束程式，不會將整個目錄掃描完

未來
   -1. scandir也可修正為function pointer傳入，可將此架構用於其他方向   
   
traveldir若實作為不傳入function pointer
優點：可讀性較佳，較容易讓別人利用，學習曲線低
缺點：抽象性不足，reuse較低
   -1. 若裡面的邏輯，handling, breakcondition修改，traveldir需要重寫，並定義一個新的traveldir function
   -2. 若要把traveldir包裝為shared library，無法包裝
*/ 


#include "stat.h"

#ifdef _INDEV
	#define log_msg(format, ...); printf(format,##__VA_ARGS__);printf("\n");
#else
	extern void log_msg(const char *fmt, ...);
#endif

#define stevedetaildebug(format, ...) {}//log_msg(format,##__VA_ARGS__);
#define stevedebug(format, ...) log_msg(format,##__VA_ARGS__);
#define statinfo(format, ...)   log_msg(format,##__VA_ARGS__);
#define DISABLEABLE_STAT

//input
space_check spacechecker;

traveldir_handler camhandler;
traveldir_handler dayhandler;
traveldir_handler filehandler;

traveldir_handler camexceedsizehandler;
traveldir_handler dayexceedsizehandler;
traveldir_handler fileexceedsizehandler;
//#define TIME_DURATION 31536000 //one year in seconds
#define TIME_DURATION 86400 //1 days in seconds
#define SPACE_THRESHOLD 90
#define FILE_EXPIRED_CHECKED_PATH "/tmp/disk/sda2"
#define DEVICE_MOUNT_PATH "/tmp/disk/sda2"
#define CAM_264_FILENAME_REGEXP_PATTERN "[\\w]*-cam[0-9A-Za-z]*-([0-9]{8})-([0-9]{6}).264"
#define CAM_264_FILENAME_REGEXP_FILTER  "[\\w]*-cam[0-9A-Za-z]*-[0-9]{8}-[0-9]{6}.264"
#define EXECUTE_FREQUENCY 5
#define STATDEVICE_PATH "/mnt/log/statDevice"

#define STAT_OK   1
#define STAT_NOK  0

#define MAX_MATCH_NUM 32
int match(const char *regex, const char *string, char *result[]) // important: remember call matchfree to free result in your code if it's not null.
{
	regex_t preg;
	int r;
	regmatch_t subs[MAX_MATCH_NUM];
	int nMatched, len;
	int i;

	r = regcomp(&preg, regex, REG_EXTENDED);
	if(r)
		return -1;

	r = regexec(&preg, string, MAX_MATCH_NUM, subs, 0);
	if(!r)
	{
		
		nMatched = preg.re_nsub + 1;
		nMatched = nMatched > MAX_MATCH_NUM ? MAX_MATCH_NUM : nMatched;
		if(result != NULL)
		{
			for(i = 0; i < nMatched; ++i)
			{
				len = subs[i].rm_eo - subs[i].rm_so;
				result[i] = (char *)malloc((len + 1) * sizeof(char));
				memcpy(result[i], string + subs[i].rm_so, len);
				result[i][len] = '\0';
			}
		}
		regfree(&preg);
		return nMatched;
	}
	else if(r == REG_NOMATCH)
	{
		regfree(&preg);
		return 0;
	}
	else
		return -1;
}

void matchfree(int nMatched, char *result[])
{
	int i;
	for(i = 0; i < nMatched; ++i)
		free(result[i]);
}

int statDevice(const char *devicemountpath)
{
#define BUFFER_SIZE	100
	char stSize[BUFFER_SIZE];stSize[0]=0;
	char strCmd[100];strCmd[0]=0;

	strcat(strCmd, "df | grep ");
	strcat(strCmd, devicemountpath);
	strcat(strCmd, " | awk '{print $5}' | sed 's/%//g' > ");
	strcat(strCmd, STATDEVICE_PATH);
	system(strCmd);
	FILE * pFile = fopen(STATDEVICE_PATH,"r");
	if(pFile == NULL)
	{
		statinfo("open statDevice file fail:%s",strerror(errno));
		return STAT_NOK;
	}
	if(fgets(stSize,BUFFER_SIZE, pFile))
	{
		stSize[strlen(stSize)-1]=0;
		stevedebug("device:%s, usage %s%%",devicemountpath, stSize);
	}
	fclose(pFile);

	if(match("[0-9]",stSize,NULL)<=0)
	{
		statinfo("statDevice error:device:%s input (%s)",devicemountpath,stSize);
		statinfo("df info:");
		strcpy(strCmd,"df > ");
		system(strcat(strCmd,STATDEVICE_PATH));
		
		pFile = fopen(STATDEVICE_PATH,"r");
		if(pFile == NULL)
		{
			statinfo("open statDevice file fail:%s",strerror(errno));
			return STAT_NOK;
		}
		while(fgets(stSize,BUFFER_SIZE, pFile))
		{
			stSize[strlen(stSize)-1]=0;
			statinfo("%s",stSize);
		}
		fclose(pFile);
		
		return -1;
	}
	return atoi(stSize);
}

int readconfig()
{
	spacechecker._time_duration=TIME_DURATION;
	spacechecker._space_threashold=SPACE_THRESHOLD;
	strcpy(spacechecker._file_expired_checked_path, FILE_EXPIRED_CHECKED_PATH);
	return STAT_OK;
}

int GetExpiredPath(char *dayPathExpired, char *timePathExpired)
{
	time_t nowtime;
	time_t expiredtime;
	struct tm *expiredtime_tm;
	struct tm *systemtime_tm;char systemtime[50];
	
	nowtime=time(NULL);systemtime_tm = localtime(&nowtime);strftime(systemtime, 50,"%Y\%m\%d %H:%M:%S",systemtime_tm);stevedebug("systemtime:%s(%lld)",systemtime,(long long int)nowtime);
	expiredtime = nowtime-TIME_DURATION;
	expiredtime_tm = localtime(&expiredtime);
	strftime(dayPathExpired, 10,"%Y%m%d",expiredtime_tm);
	stevedebug("exipred:%s",dayPathExpired);
	strftime(timePathExpired, 10,"%H%M%S",expiredtime_tm);
	stevedebug("exipred time:%s",timePathExpired);
	
	return STAT_OK;
}

int traveldir(const char *path, struct dirent ***namelist, 
              const traveldir_handler *handler)			  
{
	char handlingpath[256];
	int scandircount, idx, rethandling =-1;
	scandircount=scandir(path, namelist, handler->Filter, handler->compar);
	stevedetaildebug("scandir:%s,%d",path,scandircount);
	if(scandircount <= 0)
		return STAT_NOK;
	
	idx=0;
	while(idx<scandircount && 
	        ((handler->breakchecker.breakcondition==NULL)? 1 : 
			       (handler->breakchecker.breakcondition((*namelist)[idx]->d_name, rethandling,
				                                         &handler->breakchecker)==1)
			)
		)
	{
		strcpy(handlingpath,path);
		strcat(handlingpath,"/");
		strcat(handlingpath,(*namelist)[idx]->d_name);stevedetaildebug("handling:%s",handlingpath);
		rethandling = handler->handling(handlingpath, handler->nexthandler);
		idx++;
	}
	free(*namelist);
	return STAT_OK;
}
int fileModifydateCompar(const struct dirent **e1, const struct dirent **e2)
{
	char *matchresult1[3];
	char *matchresult2[3];
	int matchn1, matchn2;
	stevedetaildebug("e1:%s, e2:%s",(*e1)->d_name,(*e2)->d_name);
	matchn1 = match(CAM_264_FILENAME_REGEXP_PATTERN,(*e1)->d_name,matchresult1);
	if(matchn1<=0)
	{
		statinfo("fileModifydateCompar match1 error: input %s, pattern %s",(*e1)->d_name,CAM_264_FILENAME_REGEXP_PATTERN);
	}
	matchn2 = match(CAM_264_FILENAME_REGEXP_PATTERN,(*e2)->d_name,matchresult2);
	if(matchn2<=0)
	{
		statinfo("fileModifydateCompar match2 error: input %s, pattern %s",(*e2)->d_name,CAM_264_FILENAME_REGEXP_PATTERN);
	}

	stevedetaildebug("e1_match:%s, e2_match:%s",matchresult1[2],matchresult2[2]);
	return strcmp(matchresult1[2],matchresult2[2]);
}
int fileFilter(const struct dirent *namelist)
{
	if(match(CAM_264_FILENAME_REGEXP_FILTER, namelist->d_name, NULL)>0)
		return STAT_OK;
	else
		return STAT_NOK;//fail
}
int fileexceedsizebreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker)
{
	if(rethandling!=STAT_OK)
		return STAT_OK;
	else
		return STAT_NOK;
}
int filebreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker)
{
	char *matchresult[3];
	int matchn;
	
	matchn = match(CAM_264_FILENAME_REGEXP_PATTERN,checkPath,matchresult);
	if(matchn<=0)
	{
		statinfo("filebreakcondition match error: input %s, pattern %s",checkPath,CAM_264_FILENAME_REGEXP_PATTERN);
	}
	stevedetaildebug("filebreakcondition:matchresult1:%s, ExpiredDate:%s matchresult2:%s, ExpiredTime:%s",matchresult[1], checker->dayexpiredpath, matchresult[2], checker->timeexpiredpath);
	if(strcmp(matchresult[1], checker->dayexpiredpath)<0 || (strcmp(matchresult[1], checker->dayexpiredpath)>=0 && strcmp(matchresult[2], checker->timeexpiredpath)<=0))
		return STAT_OK;
	else
		return STAT_NOK;
}

int FileHandling(const char *filePath, const traveldir_handler *handler)
{
	struct stat st;
	if(lstat(filePath, &st)<0)
	{
		statinfo("lstat err:%s",strerror(errno));
	}
	else
	{
		#ifndef DISABLEABLE_STAT
		if(unlink(filePath)!=0)
		{
			statinfo("delete fail %s:%s",filePath, strerror(errno));
			return STAT_NOK
		}
		#endif
		statinfo("delete %s",filePath);
	}
	return STAT_OK;
}

int dayFilter(const struct dirent *namelist)
{
	if(match("^[0-9]{8}$", namelist->d_name, NULL)>0)
		return STAT_OK;
	else
		return STAT_NOK;//fail
}
int dayexceedsizebreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker)
{
	if(rethandling!=STAT_OK)
		return STAT_OK;
	else
		return STAT_NOK;
}
int daybreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker)
{
	if(strcmp(checkPath, checker->dayexpiredpath)<=0)
		return STAT_OK;
	else
		return STAT_NOK;
}
int DayExceedSizeHandling(const char *filePath, const traveldir_handler *handler)
{
	struct dirent **pDayDir;
	int rethandling=-1;
	rethandling = traveldir(filePath, &pDayDir, handler);
	
	return rethandling;
}
int DayHandling(const char *filePath, const traveldir_handler *handler)
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, handler);
	if(rmdir(filePath)!=0)
	{
		statinfo("rmdir fail %s:%s",filePath, strerror(errno));
	}
	return STAT_OK;
}

int camFilter(const struct dirent *namelist)
{
	if(match("cam[0-9A-Za-z]+", namelist->d_name, NULL)>0)
		return STAT_OK;
	else
		return STAT_NOK;//fail

}
int cambreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition *checker)
{
	return STAT_OK;
}
int CamExceedSizeHandling(const char *filePath, const traveldir_handler *handler)
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, handler);
	
	return STAT_OK;
}			  
int CamHandling(const char *filePath, const traveldir_handler *handler)
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, handler);
	
	return STAT_OK;
}

int checkcapacity()
{
	int pcent = statDevice(DEVICE_MOUNT_PATH);
	if(pcent < 0)
	{//statDevice error, ignore checking capacity process. retry next run
		return STAT_OK;
	}

	if(pcent >= SPACE_THRESHOLD)
	{
		stevedebug("pcent:%d exceed threashold %d delete a file every cam",pcent,SPACE_THRESHOLD);
		return STAT_NOK;//exceed threashold
	}
	stevedebug("pcent:%d capacity is ok",pcent);
	return STAT_OK;
}

void initstruct()
{
	char dayPathExpired[256],timePathExpired[256];
	GetExpiredPath(dayPathExpired, timePathExpired);
	
	camhandler.Filter=camFilter;
	camhandler.compar=alphasort;
	camhandler.handling=CamHandling;
	camhandler.breakchecker.breakcondition=cambreakcondition;
	camhandler.breakchecker.dayexpiredpath=&dayPathExpired[0];
	camhandler.breakchecker.timeexpiredpath=&timePathExpired[0];
	
	dayhandler.Filter=dayFilter;
	dayhandler.compar=alphasort;
	dayhandler.handling=DayHandling;
	dayhandler.breakchecker.breakcondition=daybreakcondition;
	dayhandler.breakchecker.dayexpiredpath=&dayPathExpired[0];
	dayhandler.breakchecker.timeexpiredpath=&timePathExpired[0];
	
	filehandler.Filter=fileFilter;
	filehandler.compar=fileModifydateCompar;
	filehandler.handling=FileHandling;
	filehandler.breakchecker.breakcondition=filebreakcondition;
	filehandler.breakchecker.dayexpiredpath=&dayPathExpired[0];
	filehandler.breakchecker.timeexpiredpath=&timePathExpired[0];
	
	camhandler.nexthandler = &dayhandler;
	dayhandler.nexthandler = &filehandler;

	camexceedsizehandler.Filter=camFilter;
	camexceedsizehandler.compar=alphasort;
	camexceedsizehandler.handling=CamExceedSizeHandling;
	camexceedsizehandler.breakchecker.breakcondition=cambreakcondition;
	camexceedsizehandler.breakchecker.dayexpiredpath=&dayPathExpired[0];
	camexceedsizehandler.breakchecker.timeexpiredpath=&timePathExpired[0];
	
	dayexceedsizehandler.Filter=dayFilter;
	dayexceedsizehandler.compar=alphasort;
	dayexceedsizehandler.handling=DayExceedSizeHandling;
	dayexceedsizehandler.breakchecker.breakcondition=dayexceedsizebreakcondition;
	dayexceedsizehandler.breakchecker.dayexpiredpath=&dayPathExpired[0];
	dayexceedsizehandler.breakchecker.timeexpiredpath=&timePathExpired[0];
	
	fileexceedsizehandler.Filter=fileFilter;
	fileexceedsizehandler.compar=fileModifydateCompar;
	fileexceedsizehandler.handling=FileHandling;
	fileexceedsizehandler.breakchecker.breakcondition=fileexceedsizebreakcondition;
	fileexceedsizehandler.breakchecker.dayexpiredpath=&dayPathExpired[0];
	fileexceedsizehandler.breakchecker.timeexpiredpath=&timePathExpired[0];	
	
	camexceedsizehandler.nexthandler = &dayexceedsizehandler;
	dayexceedsizehandler.nexthandler = &fileexceedsizehandler;
}

void deletefilesizeexceeding()
{
	struct dirent **pCamDir;
	char dayPathExpired[256],timePathExpired[256];
	pthread_detach(pthread_self());
	
	//GetExpiredPath(dayPathExpired, timePathExpired);
    readconfig();
	
	while(checkcapacity()==STAT_NOK)
	{
		traveldir(spacechecker._file_expired_checked_path,&pCamDir, &camexceedsizehandler);
	}
}

void deletefileexpired()
{
	struct dirent **pCamDir;
	char dayPathExpired[256],timePathExpired[256];
	pthread_detach(pthread_self());
	
	//GetExpiredPath(dayPathExpired, timePathExpired);
    readconfig();
	traveldir(spacechecker._file_expired_checked_path,&pCamDir, &camhandler);
}


void * deletefilethread(void* arg)
{
	initstruct();
	while(1)
	{
		stevedebug("deletefileexpired");
		deletefileexpired();
		stevedebug("deletefilesizeexceeding");
		deletefilesizeexceeding();
		sleep(EXECUTE_FREQUENCY);
	}
}

#ifdef _INDEV
int main(int argc, char *argv[])
{
	initstruct();
	stevedebug("deletefileexpired");
	deletefileexpired();
	stevedebug("deletefilesizeexceeding");
	deletefilesizeexceeding();
    return 1;
} /* main */
#endif