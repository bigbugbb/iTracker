//
//  PreviewVideoRenderer.cpp
//  QvodPlayer
//
//  Created by bigbug on 12-2-17.
//  Copyright (c) 2012年 qvod. All rights reserved.
//

#include <iostream>
#include "PreviewVideoRenderer.h"
#include "GUIDs.h"
#include "FFmpegData.h"


CPreviewVideoRenderer::CPreviewVideoRenderer(const GUID& guid, IDependency* pDepend, int* pResult)
    : CVideoRenderer(guid, pDepend, pResult)
{
}

CPreviewVideoRenderer::~CPreviewVideoRenderer()
{
}

// IPreviewVideoRenderer


int CPreviewVideoRenderer::Load()
{
    Log("CPreviewVideoRenderer::Load\n");
    EnableCaptureFrame(TRUE);
    
    return CVideoRenderer::Load();
}

int CPreviewVideoRenderer::WaitForResources(BOOL bWait)
{
    Log("CPreviewVideoRenderer::WaitForResources\n");
    
    return CVideoRenderer::WaitForResources(bWait);
}

int CPreviewVideoRenderer::Idle()
{
    Log("CPreviewVideoRenderer::Idle\n");
    
    return CVideoRenderer::Idle();
}

int CPreviewVideoRenderer::Execute()
{
    Log("CPreviewVideoRenderer::Execute\n");
    
    return CVideoRenderer::Execute();
}

int CPreviewVideoRenderer::Pause()
{
    Log("CPreviewVideoRenderer::Pause\n");
    
    return CVideoRenderer::Pause();
}

int CPreviewVideoRenderer::BeginFlush()
{
    Log("CPreviewVideoRenderer::BeginFlush\n");
    
    return CVideoRenderer::BeginFlush();
}

int CPreviewVideoRenderer::EndFlush()
{
    Log("CPreviewVideoRenderer::EndFlush\n");
    
    return CVideoRenderer::EndFlush();
}

int CPreviewVideoRenderer::Invalid()
{
    Log("CPreviewVideoRenderer::Invalid\n");
    
    return CVideoRenderer::Invalid();
}

int CPreviewVideoRenderer::Unload()
{
    Log("CPreviewVideoRenderer::Unload\n");
    
    return CVideoRenderer::Unload();
}

int CPreviewVideoRenderer::SetEOS()
{
    Log("CPreviewVideoRenderer::SetEOS\n");
    
    return CVideoRenderer::SetEOS();
}

int CPreviewVideoRenderer::GetSamplePool(const GUID& guid, ISamplePool** ppPool)
{
    AssertValid(ppPool);
    
    if (!memcmp(&guid, &GUID_PREVIEW_VIDEO_DECODER, sizeof(GUID))) {
        *ppPool = &m_FramePool;
    } else {
        *ppPool = NULL;
    }
    
    return S_OK;
}

inline
void CPreviewVideoRenderer::DeliverFrame(CFrame* pFrame)
{
    m_pCapturer->CaptureFrame(this, &pFrame->m_frame);
}

THREAD_RETURN CPreviewVideoRenderer::ThreadProc()
{
    return CVideoRenderer::ThreadProc();
}


