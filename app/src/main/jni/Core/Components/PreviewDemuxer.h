//
//  PreviewDemuxer.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodPlayer_PreviewDemuxer_h
#define QvodPlayer_PreviewDemuxer_h

#include <string.h>
using std::string;

#include "FFmpegDemuxer.h"

class CPreviewDemuxer : public CFFmpegDemuxer
{
public:
    CPreviewDemuxer(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CPreviewDemuxer();
    
    virtual int InitialConfig(const char* szURL, float fOffset, BOOL bRemote);
    
protected:
    // CMediaObject
    int Load();
    int WaitForResources(BOOL bWait);
    int Idle();
    int Execute();
    int Pause();
    int BeginFlush();
    int EndFlush();
    int Invalid();
    int Unload();
    int Release();
    int SetEOS();
    int GetOutputPool(const GUID& requestor, ISamplePool** ppPool);

    virtual THREAD_RETURN ThreadProc();
    
    virtual int RebuildIndexEntries(AVFormatContext* pFmtCtx, AVCodecContext* pVideoCtx, AVCodecContext* pAudioCtx);
};

#endif
