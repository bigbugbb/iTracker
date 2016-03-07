//
//  Frame.cpp
//  QVOD
//
//  Created by bigbug on 11-12-1.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "Frame.h"
#include "FFmpegData.h"

CFrame::CFrame()
{
    m_nType     = 0;
    m_bShow     = FALSE;
    m_nDuration = 0;
    m_nWidth    = 0;
    m_nHeight   = 0;
    
    avcodec_get_frame_defaults(&m_frame);
}

CFrame::~CFrame()
{
    Free();
}

int CFrame::Resize(int nWidth, int nHeight, enum PixelFormat ePixFmt)
{
	Log("CFrame::Resize, width: %d, height: %d\n", nWidth, nHeight);
    int nResult = S_OK;

    m_ePixFmt = ePixFmt;

    Free();
    if ((nResult = Alloc(nWidth, nHeight)) != S_OK) {
    	Log("CFrame::Alloc failed\n");
        return nResult;
    }
    
    Log("CFrame::Resize end");
    return S_OK;
}

int CFrame::Alloc(int nWidth, int nHeight)
{
    AssertValid(nWidth > 0 && nHeight > 0);
//    int nSize = avpicture_get_size(m_ePixFmt, nWidth, nHeight);
//    
//    if ((m_pFrame = avcodec_alloc_frame()) == NULL) {
//        return E_OUTOFMEMORY;
//    }
//    if ((m_pData = (BYTE*)align_malloc(nSize, nAlign)) == NULL) {
//        av_free(m_pFrame); m_pFrame = NULL;
//        return E_OUTOFMEMORY;
//    }
//    
//    if (avpicture_fill((AVPicture*)m_pFrame, m_pData, m_ePixFmt, nWidth, nHeight) < 0) {
//        return E_FAIL;
//    }
    if (avpicture_alloc((AVPicture*)&m_frame, m_ePixFmt, nWidth, nHeight) < 0) {
        return E_FAIL;
    }

    m_nWidth  = nWidth;
    m_nHeight = nHeight;
    m_frame.width  = nWidth;
    m_frame.height = nHeight;
    m_frame.format = m_ePixFmt;
    
    return S_OK;
}

void CFrame::Free()
{
    if (m_frame.data[0]) {
    	avpicture_free((AVPicture*)&m_frame);
    	m_frame.data[0] = NULL;
    }

    avcodec_get_frame_defaults(&m_frame);
}


