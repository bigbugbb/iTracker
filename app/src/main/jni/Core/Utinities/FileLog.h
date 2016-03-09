//
//  FileLog.h
//  QvodBrowser
//
//  Created by qvod on 12-3-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodBrowser_FileLog_h
#define QvodBrowser_FileLog_h


int  OpenLogFile(const char *path);
int	 LogToFile(const char * __restrict, ...);
int  CloseLogFile();
#endif
