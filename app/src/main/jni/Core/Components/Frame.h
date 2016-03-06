//
//  Frame.h
//  QVOD
//
//  Created by bigbug on 11-11-26.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Frame_h
#define QVOD_Frame_h

#include "Global.h"
#include "../DependencyObject.h"
#include "FFmpegData.h"

class CFramePool;

class CFrame : public CDependencyObject
{
    friend class CFramePool;
public:
    CFrame();
    virtual ~CFrame();
    
    int Resize(int nWidth, int nHeight, enum AVPixelFormat ePixFmt);
    
    int           m_nType;
    BOOL          m_bShow;
    int           m_nDuration;
    int           m_nWidth;
    int           m_nHeight;
    AVFrame       m_frame;
    AVPixelFormat m_ePixFmt;

protected:
    int  Alloc(int nWidth, int nHeight);
    void Free();
    void Reset();
};

#endif
