
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

#ifdef _INDEV
	#define log_msg(format, ...); printf(format,##__VA_ARGS__);printf("\n");
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
#define FILE_EXPIRED_CHECKED_PATH "/tmp/disk/sda2"
#define DEVICE_MOUNT_PATH "/tmp/disk/sda2"
#define CAM_264_FILENAME_REGEXP_PATTERN "[\\w\\W]*-cam[0-9A-Za-z]*-([0-9]{8})-([0-9]{6}).264"
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

int statDevice(char *devicemountpath)
{
#define BUFFER_SIZE	20
	char stSize[BUFFER_SIZE];
	char strCmd[100];strCmd[0]=0;
	int ret=0;
	char *strtonumerrst;
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
	/*
	ret = strtonum(stSize, 0, 100, &strtonumerrst);
	if(strtonumerrst!=NULL)
	{
		statinfo("strtonum error:%s, input:%s",strtonumerrst,stSize);
		return -1;
	}*/
	if(match("[0-9]",stSize,NULL)<=0)
	{
		statinfo("statDevice error:input %s",stSize);
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
	
	nowtime=time(NULL);
	expiredtime = nowtime-TIME_DURATION;
	expiredtime_tm = localtime(&expiredtime);
	strftime(dayPathExpired, 10,"%Y%m%d",expiredtime_tm);
	stevedebug("exipred:%s",dayPathExpired);
	strftime(timePathExpired, 10,"%H%M%S",expiredtime_tm);
	stevedebug("exipred time:%s",timePathExpired);
	
	return STAT_OK;
}
  
int traveldir(char *path, struct dirent ***namelist, char *ExpiredDate, char *ExpiredTime,
              int (*Filter)(const struct dirent *), 
			  int (*compar)(const struct dirent **, const struct dirent **), 
			  int (*handling)(char *filepath, char*, char*,int (*breakcondition)(char *, char*,char*, int, int)),
			  int (*breakcondition)(char *, char*,char*, int, int))
{
	char handlingpath[256];
	int scandircount, idx, rethandling=-1;
	scandircount=scandir(path, namelist, Filter, compar);
	stevedetaildebug("scandir:%s,%d",path,scandircount);
	if(scandircount <= 0)
		return STAT_NOK;
	
	idx=0;
	while(idx<scandircount && breakcondition((*namelist)[idx]->d_name,ExpiredDate,ExpiredTime,idx,rethandling)==1)
	{
		strcpy(handlingpath,path);
		strcat(handlingpath,"/");
		strcat(handlingpath,(*namelist)[idx]->d_name);stevedetaildebug("handling:%s",handlingpath);
		rethandling = handling(handlingpath, ExpiredDate, ExpiredTime, breakcondition);
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
	if(match("[\\w\\W]*-cam[0-9A-Za-z]*-[0-9]{8}-[0-9]{6}.264", namelist->d_name, NULL)>0)
		return STAT_OK;
	else
		return STAT_NOK;//fail
}
int fileexceedsizebreakcondition(char *checkPath, char *ExpiredDate,char *ExpiredTime, int idx, int uplimit)
{
	if(uplimit!=STAT_OK)
		return STAT_OK;
	else
		return STAT_NOK;
}
int filebreakcondition(char *checkPath, char *ExpiredDate,char *ExpiredTime, int idx, int uplimit)
{
	char *matchresult[3];
	int matchn;
	
	matchn = match(CAM_264_FILENAME_REGEXP_PATTERN,checkPath,matchresult);
	if(matchn<=0)
	{
		statinfo("filebreakcondition match error: input %s, pattern %s",checkPath,CAM_264_FILENAME_REGEXP_PATTERN);
	}
	stevedetaildebug("filebreakcondition:matchresult1:%s, ExpiredDate:%s matchresult2:%s, ExpiredTime:%s",matchresult[1], ExpiredDate, matchresult[2], ExpiredTime);
	if(strcmp(matchresult[1], ExpiredDate)<0 || (strcmp(matchresult[1], ExpiredDate)>=0 && strcmp(matchresult[2], ExpiredTime)<=0))
		return STAT_OK;
	else
		return STAT_NOK;
}

int FileHandling(char *filePath, char *ExpiredDate, char *ExpiredTime,
                int (*breakcondition)(char *, char*, char*, int, int))
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
int dayexceedsizebreakcondition(char *checkPath, char *ExpiredDate,char *ExpiredTime, int idx, int uplimit)
{
	if(uplimit!=STAT_OK)
		return STAT_OK;
	else
		return STAT_NOK;
}
int daybreakcondition(char *checkPath, char *ExpiredDate,char *ExpiredTime, int idx, int uplimit)
{
	if(strcmp(checkPath, ExpiredDate)<=0)
		return STAT_OK;
	else
		return STAT_NOK;
}
int DayExceedSizeHandling(char *filePath, char *ExpiredDate, char *ExpiredTime,
                int (*breakcondition)(char *, char*, char*, int, int))
{
	struct dirent **pDayDir;
	int rethandling=-1;
	rethandling = traveldir(filePath, &pDayDir,ExpiredDate, ExpiredTime, fileFilter, fileModifydateCompar, FileHandling, fileexceedsizebreakcondition);
	
	return rethandling;
}
int DayHandling(char *filePath, char *ExpiredDate, char *ExpiredTime,
                int (*breakcondition)(char *, char*, char*, int, int))
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir,ExpiredDate, ExpiredTime, fileFilter, fileModifydateCompar, FileHandling, filebreakcondition);
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
int cambreakcondition(char *checkPath, char *ExpiredDate,char *ExpiredTime, int idx, int uplimit)
{
	return STAT_OK;
}
int CamExceedSizeHandling(char *filePath, char *ExpiredDate, char *ExpiredTime,
                int (*breakcondition)(char *, char*, char*, int, int))
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, ExpiredDate, ExpiredTime, dayFilter, alphasort, DayExceedSizeHandling, dayexceedsizebreakcondition);
	
	return STAT_OK;
}			  
int CamHandling(char *filePath, char *ExpiredDate, char *ExpiredTime,
                int (*breakcondition)(char *, char*, char*, int, int))
{
	struct dirent **pDayDir;
	traveldir(filePath, &pDayDir, ExpiredDate, ExpiredTime, dayFilter, alphasort, DayHandling, daybreakcondition);
	
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

void * deletefilesizeexceeding()
{
	struct dirent **pCamDir;
	char dayPathExpired[256],timePathExpired[256];
	pthread_detach(pthread_self());
	
	GetExpiredPath(dayPathExpired, timePathExpired);
    readconfig();
	
	while(checkcapacity()==STAT_NOK)
	{
		traveldir(spacechecker._file_expired_checked_path,&pCamDir, dayPathExpired, timePathExpired,
				   camFilter, alphasort, CamExceedSizeHandling, cambreakcondition);
	}
}

void * deletefileexpired()
{
	struct dirent **pCamDir;
	char dayPathExpired[256],timePathExpired[256];
	pthread_detach(pthread_self());
	
	GetExpiredPath(dayPathExpired, timePathExpired);
    readconfig();
	traveldir(spacechecker._file_expired_checked_path,&pCamDir, dayPathExpired, timePathExpired,
	           camFilter, alphasort, CamHandling, cambreakcondition);
}


void * deletefilethread(void* arg)
{
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
	stevedebug("deletefileexpired");
	deletefileexpired();
	stevedebug("deletefilesizeexceeding");
	deletefilesizeexceeding();
    return 1;
} /* main */
#endif