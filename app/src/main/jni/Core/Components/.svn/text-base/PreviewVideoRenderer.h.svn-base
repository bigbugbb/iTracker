//
//  PreviewVideoRenderer.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodPlayer_PreviewVideoRenderer_h
#define QvodPlayer_PreviewVideoRenderer_h

#include "VideoRenderer.h"

class CPreviewVideoRenderer : public CVideoRenderer
{
public:
    CPreviewVideoRenderer(const GUID& guid, IDependency* pDepend, int* pResult);
    virtual ~CPreviewVideoRenderer();
    
    // IPreviewVideoRenderer
  
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
    int SetEOS();
    int GetSamplePool(const GUID& guid, ISamplePool** ppPool);
    
    virtual THREAD_RETURN ThreadProc();
    
    virtual void DeliverFrame(CFrame* pFrame);
};

#endif
