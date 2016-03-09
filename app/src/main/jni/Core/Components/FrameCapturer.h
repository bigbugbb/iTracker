//
//  FrameCapturer.h
//  QvodPlayer
//
//  Created by bigbug on 12-2-21.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#ifndef QvodPlayer_FrameCapturer_h
#define QvodPlayer_FrameCapturer_h

#include "../Message.h"
#include "../MediaObject.h"
#include "CompInterfaces.h"
#include <vector>
using std::vector;

class CFrameCapturer : public CThread,
                       public CDependencyObject
{
public:
    CFrameCapturer(IDependency* pDepend);
    virtual ~CFrameCapturer();
    
    static CFrameCapturer* GetInstance(IDependency* pDepend);
    
    int  CaptureFrame(CMediaObject* pSender, void* pData);
    void SetCaptureFormat(AVPixelFormat eFormat);
    
protected:
    int SendMessage(const Message& msg);
    int RecvMessage(Message& msg);
    
    virtual THREAD_RETURN ThreadProc();
    
    CLock             m_csCapture;
    CEvent            m_etCapture;
    AVPixelFormat     m_eDstFmt;
//    SwsContext*       m_pSwsCtx;
    CMessageQueue     m_MsgQueue;
};

#endif
