//
//  FileLog.cpp
//  QvodBrowser
//
//  Created by qvod on 12-3-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include "FileLog.h"
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
static FILE *g_log_file = NULL; 

int  OpenLogFile(const char *path)
{
    if (g_log_file) {
        return -1;
    }
    int nSize = strlen(path) + 20;
    char *buffer = new char[nSize];
    memset(buffer, 0, nSize);
    strcpy(buffer, path);
    strcat(buffer, "/qvodplayer_log.txt");
    
    g_log_file = fopen(buffer,"w");
    delete []buffer;
    return 0;
}

int	 LogToFile(const char * fmt, ...)
{
    if (!g_log_file) {
        return -1;
    }
    va_list args;
	int n;
	va_start(args, fmt);
	n = vfprintf(g_log_file, fmt, args);
	va_end(args);
	return n;
}
int  CloseLogFile()
{
    if (g_log_file) {
        fclose(g_log_file);
        g_log_file = NULL;
    }
    return 0;
}