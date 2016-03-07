//
//  MediaObject.cpp
//  QVOD
//
//  Created by bigbug on 11-11-13.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>
#include "MediaObject.h"
#include "SysConsts.h"


CMediaObject::CMediaObject(const GUID& guid, IDependency* pDepend)
    : CDependencyObject(pDepend), m_pRefClock(NULL)
{
    memcpy(&m_guid, &guid, sizeof(GUID));
    
    m_bEOS    = FALSE;
    m_bFlush  = FALSE;
    m_bEnable = TRUE;
    m_eType   = TRANSFORM;
    m_nState  = STATE_UNLOADED;
}

CMediaObject::~CMediaObject()
{
    
}

int CMediaObject::Load()
{
    m_bEOS = FALSE;
    SetState(STATE_LOADED, STATE_UNLOADED);
    return S_OK;
}

int CMediaObject::WaitForResources(BOOL bWait)
{
    if (bWait) {
        SetState(STATE_WAITFORRESOURCES, 0);
    } else {
        SetState(0, STATE_WAITFORRESOURCES);
    }
    
    return S_OK;
}

int CMediaObject::Idle()
{
    SetState(STATE_IDLE, STATE_WAITFORRESOURCES | STATE_EXECUTE | STATE_PAUSE);
    return S_OK;
}

int CMediaObject::Execute()
{
    SetState(STATE_EXECUTE, STATE_IDLE | STATE_PAUSE);
    return S_OK;
}

int CMediaObject::Pause()
{
    SetState(STATE_PAUSE, STATE_IDLE | STATE_EXECUTE);
    return S_OK;
}

int CMediaObject::BeginFlush()
{
    m_bFlush = TRUE;
    return S_OK;
}

int CMediaObject::EndFlush()
{
    m_bEOS   = FALSE;
    m_bFlush = FALSE; 
    return S_OK;
}

int CMediaObject::Invalid()
{
    SetState(STATE_INVALID, STATE_WAITFORRESOURCES | STATE_IDLE | STATE_EXECUTE | STATE_PAUSE);
    return S_OK;
}

int CMediaObject::Unload()
{
    m_bEnable = TRUE;
    SetState(STATE_UNLOADED, STATE_LOADED | STATE_WAITFORRESOURCES | 
             STATE_IDLE | STATE_EXECUTE | STATE_PAUSE | STATE_INVALID);
    return S_OK;
}

int CMediaObject::Release()
{
    return S_OK;
}

const GUID& CMediaObject::GetGUID()
{
    return m_guid;
}

int CMediaObject::GetState()
{
    CAutoLock cObjectLock(&m_csState);
    
    return m_nState;
}

int CMediaObject::SetState(int nAdd, int nRemove)
{
    CAutoLock cObjectLock(&m_csState);
    
    m_nState &= ~nRemove;
    m_nState |= nAdd;
    
    return m_nState;
}

int CMediaObject::SetEOS()
{
    m_bEOS = TRUE;
 
    return S_OK;
}

int CMediaObject::Enable(BOOL bEnable)
{
    m_bEnable = bEnable;
    
    return S_OK;
}

int CMediaObject::Receive(ISamplePool* pPool)
{
    CMediaSample sample;
    
    int nResult = pPool->GetUnused(sample);
    if (nResult != S_OK) {
        return E_RETRY;
    }
    
    nResult = OnReceive(sample);

    if (nResult != E_RETRY) {
        pPool->Recycle(sample);
    }
    
    return nResult;
}

int CMediaObject::Dispatch(const GUID& receiver, int nType, void* pUserData)
{
    CMediaObject* pObj = NULL;
    
    for (int i = 0; i < m_vecOutObjs.size(); ++i) {
        AssertValid(m_vecOutObjs[i]);
        
        pObj = m_vecOutObjs[i];
        if (receiver == pObj->m_guid) {
            break;
        }
    }
    
    return pObj ? pObj->RespondDispatch(m_guid, nType, pUserData) : E_FAIL;
}

int CMediaObject::RespondDispatch(const GUID& sender, int nType, void* pUserData)
{
    return S_OK;
}

int CMediaObject::Feedback(const GUID& receiver, int nType, void* pUserData)
{
    CMediaObject* pObj = NULL;
    
    for (int i = 0; i < m_vecInObjs.size(); ++i) {
        AssertValid(m_vecInObjs[i]);
        
        pObj = m_vecInObjs[i];
        if (receiver == pObj->m_guid) {
            break;
        }
    }
 
    return pObj ? pObj->RespondFeedback(m_guid, nType, pUserData) : E_FAIL;
}

int CMediaObject::RespondFeedback(const GUID& sender, int nType, void* pUserData)
{
    return S_OK;
}

int CMediaObject::Connect(DIR eDir, CMediaObject* pObj)
{
    AssertValid(pObj);
    
    if (eDir == DIR_IN) {
        m_vecInObjs.push_back(pObj);
    } else if (eDir == DIR_OUT) {
        m_vecOutObjs.push_back(pObj);
    } else {
        AssertValid(0);
    }
    
    return S_OK;
}

int CMediaObject::Operate(int nCommand, void* pParam)
{
    int nResult;

    switch (nCommand) {
    case COMMAND_LOAD:
        nResult = Load();
        break;
    case COMMAND_WAITFORRESOURCES:
        nResult = WaitForResources(*(BOOL*)pParam);
        break;
    case COMMAND_IDLE:
        nResult = Idle();
        break;
    case COMMAND_EXECUTE:
        nResult = Execute();
        break;
    case COMMAND_PAUSE:
        nResult = Pause();
        break;
    case COMMAND_BEGINFLUSH:
        nResult = BeginFlush();
        break;
    case COMMAND_ENDFLUSH:
        nResult = EndFlush();
        break;    
    case COMMAND_INVALID:
        nResult = Invalid();
        break;
    case COMMAND_UNLOAD:
        nResult = Unload();
        break;
    case COMMAND_RELEASE:
        nResult = Release();
        break;
    default:
        nResult = E_NOIMPL;
        break;
    }
    
    return nResult;
}

int CMediaObject::GetInputPool(const GUID& requestor, ISamplePool** ppPool)
{
    *ppPool = NULL;
    
    return S_OK;
}

int CMediaObject::GetOutputPool(const GUID& requestor, ISamplePool** ppPool)
{
    *ppPool = NULL;
    
    return S_OK;
}

int CMediaObject::GetSyncSource(IReferenceClock** ppRefClock)
{
    *ppRefClock = m_pRefClock;
    
    return S_OK;
}

int CMediaObject::SetSyncSource(IReferenceClock* pRefClock)
{
    m_pRefClock = pRefClock;
    
    return S_OK;
}

int CMediaObject::OnReceive(CMediaSample& sample)
{
    return S_OK;
}

CSource::CSource(const GUID& guid, IDependency* pDepend)
    : CMediaObject(guid, pDepend)
{
    m_eType = SOURCE;
}

CSource::~CSource()
{
}

CSink::CSink(const GUID& guid, IDependency* pDepend)
    : CMediaObject(guid, pDepend)
{
    m_eType = SINK;
}

CSink::~CSink()
{
}

CMediaSample::CMediaSample()
{
    m_Type        = SAMPLE_NONE;
    m_pBuf        = NULL;
    m_pCur        = NULL;
    m_nSize       = 0;
    m_nActual     = 0;
    m_llTimestamp = -1;
    m_llSyncPoint = -1;
    m_pSpecs      = NULL;
    m_nSpecsSize  = 0;
    m_pExten      = NULL;
    m_nExtenSize  = 0;
    m_bIgnore     = FALSE;
    m_bDiscon     = FALSE;
    m_pListener   = NULL;
    m_pOwner      = NULL;
}

CMediaSample::~CMediaSample()
{
}
