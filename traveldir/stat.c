/*
用途(use case)
   -1. 巢狀處理樹狀結構，使用scandir掃描出本階層符合filter條件的枝幹，並依照compar邏輯做排序
   -2. 每個枝幹會呼叫枝幹處理Handling
   -3. 每個枝幹處理前會依照breakcondition檢查，若不需繼續輪巡所有枝幹，則提前結束本階層的Handling
架構
   traveldir為controller，程式的核心
   利用此核心需實作_traveldir_handler結構定義的function
   -1. Handling
       處理每個枝幹的工作
   -2. breakcondition
       輪巡枝幹中斷的判斷函數，若判斷成立可提前結束此層枝幹的輪巡
   -3. filter
      使用於scandir，檢查傳入的檔名是否符合命名規則
   -4. compar
      使用於scandir，排序檔案的邏輯
   -5. nexthandler
      指向下一層支幹的_traveldir_handler結構，traveldir，scan完本層所有元素後，將此指標傳到下一層支幹處理
   -6. 用來包裝breakcondition傳入的參數，提供traveldir client實作breakcondition用
   
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
	#define STATDEVICE_PATH "/mnt/log/statDevice"
	#define DISK_PATH "/mnt/log/test/"	
#else
	extern void log_msg(const char *fmt, ...);
#endif

#define stevedetaildebug(format, ...) {}//log_msg(format,##__VA_ARGS__);
#define stevedebug(format, ...) log_msg(format,##__VA_ARGS__);
#define statinfo(format, ...)   log_msg(format,##__VA_ARGS__);
#define DISABLEABLE_STAT

//input
space_check spacechecker;

//#define TIME_DURATION 31536000 //one year in seconds
#define TIME_DURATION 86400 //1 days in seconds
#define SPACE_THRESHOLD 90
#define CAM_FILENAME_REGEXP_PATTERN "[\\w]*-cam[0-9A-Za-z]*-([0-9]{8})-([0-9]{6}).[0-9A-Za-z]*"
#define CAM_FILENAME_REGEXP_FILTER  "[\\w]*-cam[0-9A-Za-z]*-[0-9]{8}-[0-9]{6}.[0-9A-Za-z]*"
#define EXECUTE_FREQUENCY 600

#define STAT_OK   1
#define STAT_NOK  0

int CamHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker);
int CamExceedSizeHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker);
int cambreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition *checker);
int camFilter(const struct dirent *namelist);

int DayHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker);
int DayExceedSizeHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker);
int daybreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker);
int dayexceedsizebreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker);
int dayFilter(const struct dirent *namelist);

int FileHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker);
int filebreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker);
int fileexceedsizebreakcondition(const char *checkPath, const int rethandling, const traveldir_breakcondition* checker);
int fileFilter(const struct dirent *namelist);
int fileModifydateCompar(const struct dirent **e1, const struct dirent **e2);

traveldir_breakcondition breakchecker;

traveldir_handler filehandler={fileFilter, fileModifydateCompar, FileHandling, filebreakcondition, NULL};
traveldir_handler dayhandler ={dayFilter,  alphasort,            DayHandling,  daybreakcondition,  &filehandler};
traveldir_handler camhandler ={camFilter,  alphasort,            CamHandling,  cambreakcondition,  &dayhandler};

traveldir_handler fileexceedsizehandler
	={fileFilter, fileModifydateCompar, FileHandling, fileexceedsizebreakcondition, NULL};
traveldir_handler dayexceedsizehandler 
	={dayFilter, alphasort, DayExceedSizeHandling, dayexceedsizebreakcondition, &fileexceedsizehandler};
traveldir_handler camexceedsizehandler 
	={camFilter, alphasort, CamExceedSizeHandling, cambreakcondition, &dayexceedsizehandler};



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

int trim_last_slash(char *s)
{
	if(s==NULL)
		return STAT_NOK;
	
	if(s[strlen(s)-1]=='/')
	{
		s[strlen(s)-1]=0;
		return STAT_OK;
	}
	else
	{
		return STAT_NOK;
	}
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
	strcpy(spacechecker._file_expired_checked_path, DISK_PATH);
	
	if(trim_last_slash(spacechecker._file_expired_checked_path)==STAT_NOK)
	{
		statinfo("_file_expired_checked_path pattern error:%s",spacechecker._file_expired_checked_path);
		return STAT_NOK;
	}

	stevedebug("_file_expired_checked_path:%s",spacechecker._file_expired_checked_path);
	return STAT_OK;
}

int GetExpiredPath(char *dayPathExpired, char *timePathExpired)
{
	time_t nowtime;
	time_t expiredtime;
	struct tm *expiredtime_tm;
	struct tm *systemtime_tm;char systemtime[50];
	
	nowtime=time(NULL);systemtime_tm = localtime(&nowtime);
	strftime(systemtime, 50,"%Y\%m\%d %H:%M:%S",systemtime_tm);
	statinfo("systemtime:%s(%lld)",systemtime,(long long int)nowtime);
	
	expiredtime = nowtime-TIME_DURATION;
	expiredtime_tm = localtime(&expiredtime);
	snprintf(dayPathExpired, 10,"%04d%02d%02d",expiredtime_tm->tm_year+1900,expiredtime_tm->tm_mon+1,expiredtime_tm->tm_mday);
	strcpy(dayPathExpired,"20160525");
	statinfo("exipred:%s",dayPathExpired);
	snprintf(timePathExpired, 10,"%02d%02d%02d",expiredtime_tm->tm_hour,expiredtime_tm->tm_min,expiredtime_tm->tm_sec);
	strcpy(timePathExpired,"000200");
	statinfo("exipred time:%s",timePathExpired);
	
	return STAT_OK;
}

int traveldir(const char *path, struct dirent ***namelist, 
              const traveldir_handler *handler, const traveldir_breakcondition *breakchecker)			  
{
	char handlingpath[256];
	int scandircount, idx, rethandling =-1;
	scandircount=scandir(path, namelist, handler->Filter, handler->compar);
	stevedetaildebug("scandir:%s,%d",path,scandircount);
	if(scandircount <= 0)
		return STAT_NOK;
	
	idx=0;
	while(idx<scandircount && 
	        ((handler->breakcondition==NULL)? 1 : 
			       (handler->breakcondition((*namelist)[idx]->d_name, rethandling, breakchecker)==1)
			)
		)
	{
		strcpy(handlingpath,path);
		strcat(handlingpath,"/");
		strcat(handlingpath,(*namelist)[idx]->d_name);stevedetaildebug("handling:%s",handlingpath);
		rethandling = handler->handling(handlingpath, handler->nexthandler, breakchecker);
		free((*namelist)[idx]);
		idx++;
	}
	free(*namelist);
	return STAT_OK;
}
int fileModifydateCompar(const struct dirent **e1, const struct dirent **e2)
{
	char *matchresult1[3];
	char *matchresult2[3];
	int matchn1, matchn2, ret;
	stevedetaildebug("e1:%s, e2:%s",(*e1)->d_name,(*e2)->d_name);
	matchn1 = match(CAM_FILENAME_REGEXP_PATTERN,(*e1)->d_name, matchresult1);
	if(matchn1<=0)
	{
		statinfo("fileModifydateCompar match1 error: input %s, pattern %s",(*e1)->d_name,CAM_FILENAME_REGEXP_PATTERN);
	}
	matchn2 = match(CAM_FILENAME_REGEXP_PATTERN,(*e2)->d_name, matchresult2);
	if(matchn2<=0)
	{
		statinfo("fileModifydateCompar match2 error: input %s, pattern %s",(*e2)->d_name,CAM_FILENAME_REGEXP_PATTERN);
	}

	stevedetaildebug("e1_match:%s, e2_match:%s",matchresult1[2],matchresult2[2]);
	ret = strcmp(matchresult1[2],matchresult2[2]);
	
	if(matchn1 > 0)
		matchfree(matchn1, matchresult1);
	
	if(matchn2 > 0)
		matchfree(matchn2, matchresult2);
	return ret;
}
int fileFilter(const struct dirent *namelist)
{
	if(match(CAM_FILENAME_REGEXP_FILTER, namelist->d_name, NULL)>0)
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
	
	matchn = match(CAM_FILENAME_REGEXP_PATTERN,checkPath,matchresult);
	if(matchn<=0)
	{
		statinfo("filebreakcondition match error: input %s, pattern %s",checkPath,CAM_FILENAME_REGEXP_PATTERN);
		return STAT_NOK;
	}
	stevedetaildebug("filebreakcondition:matchresult1:%s, ExpiredDate:%s matchresult2:%s, ExpiredTime:%s",matchresult[1], checker->dayexpiredpath, matchresult[2], checker->timeexpiredpath);

	if(strcmp(matchresult[1], checker->dayexpiredpath)<0 || (strcmp(matchresult[1], checker->dayexpiredpath)>=0 && strcmp(matchresult[2], checker->timeexpiredpath)<=0))
	{
		matchfree(matchn, matchresult);
		return STAT_OK;
	}
	else
	{
		matchfree(matchn, matchresult);
		return STAT_NOK;
	}
}

int FileHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker)
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
			return STAT_NOK;
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
int DayExceedSizeHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker)
{
	struct dirent **pDayDir;
	int rethandling=-1;
	rethandling = traveldir(filePath, &pDayDir, handler, breakchecker);
	
	return rethandling;
}
int DayHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker)
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, handler, breakchecker);
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
int CamExceedSizeHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker)
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, handler, breakchecker);
	
	return STAT_OK;
}			  
int CamHandling(const char *filePath, const traveldir_handler *handler, const traveldir_breakcondition *breakchecker)
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, handler, breakchecker);
	
	return STAT_OK;
}

int checkcapacity()
{
	char statDevicePath[50];
	strcpy(statDevicePath,DISK_PATH);
	
	if(trim_last_slash(statDevicePath)==STAT_NOK)
	{
		statinfo("statDevicePath pattern error:%s",statDevicePath);
		return STAT_NOK;
	}
	
	int pcent = statDevice(statDevicePath);
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
char dayPathExpired[256],timePathExpired[256];
void initstruct()
{
	GetExpiredPath(dayPathExpired, timePathExpired);
	
	breakchecker.dayexpiredpath=&dayPathExpired[0];
	breakchecker.timeexpiredpath=&timePathExpired[0];
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
		traveldir(spacechecker._file_expired_checked_path,&pCamDir, &camexceedsizehandler, &breakchecker);
	}
}

void deletefileexpired()
{
	struct dirent **pCamDir;
	char dayPathExpired[256],timePathExpired[256];
	pthread_detach(pthread_self());
	
	//GetExpiredPath(dayPathExpired, timePathExpired);
    readconfig();
	traveldir(spacechecker._file_expired_checked_path,&pCamDir, &camhandler, &breakchecker);
}


void * deletefilethread(void* arg)
{
	while(1)
	{
		initstruct();
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
	int i;
	for(i=0;i<10;i++)
	{
		initstruct();
		stevedebug("deletefileexpired");
		deletefileexpired();
		stevedebug("deletefilesizeexceeding");
		deletefilesizeexceeding();
	}
    return 1;
} /* main */
#endif