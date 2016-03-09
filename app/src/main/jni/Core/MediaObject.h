//
//  MediaObject.h
//  QVOD
//
//  Created by bigbug on 11-11-13.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_MediaObject_h
#define QVOD_MediaObject_h

#include "RefClock.h"
#include "ISamplePool.h"
#include "DependencyObject.h"
using namespace::ios_qvod_player;
#include <vector>
using std::vector;

#define COMMAND_LOAD                0
#define COMMAND_WAITFORRESOURCES    1
#define COMMAND_IDLE                2
#define COMMAND_EXECUTE             3
#define COMMAND_PAUSE               4
#define COMMAND_BEGINFLUSH          5
#define COMMAND_ENDFLUSH            6
#define COMMAND_INVALID             7
#define COMMAND_UNLOAD              8
#define COMMAND_RELEASE             9

typedef enum _DIR {
    DIR_IN = 0,
    DIR_OUT
} DIR;

enum ComponentType {
    SOURCE,
    TRANSFORM,
    SINK
};

class CMediaObject : public CDependencyObject
{
public:
    CMediaObject(const GUID& guid, IDependency* pDepend);
    virtual ~CMediaObject();
    
    virtual int Load();
    virtual int WaitForResources(BOOL bWait);
    virtual int Idle();
    virtual int Execute();
    virtual int Pause();
    virtual int BeginFlush();
    virtual int EndFlush();
    virtual int Invalid();
    virtual int Unload();
    virtual int Release();
    virtual int SetEOS();
    virtual int Receive(ISamplePool* pPool);
    
    virtual int Dispatch(const GUID& receiver, int nType, void* pUserData);
    virtual int RespondDispatch(const GUID& sender, int nType, void* pUserData);
    virtual int Feedback(const GUID& receiver, int nType, void* pUserData);
    virtual int RespondFeedback(const GUID& sender, int nType, void* pUserData);
    
    virtual int GetInputPool(const GUID& requestor, ISamplePool** ppPool);
    virtual int GetOutputPool(const GUID& requestor, ISamplePool** ppPool);
    virtual int SetSyncSource(IReferenceClock* pRefClock);
    virtual int GetSyncSource(IReferenceClock** ppRefClock);
    
    const GUID& GetGUID();
    int GetState();
    int Enable(BOOL bEnable = TRUE);
    int Connect(DIR eDir, CMediaObject* pObj);
    int Operate(int nCommand, void* pParam);
    
    BOOL IsEOS() const { return m_bEOS; }
    BOOL IsEnabled() const { return m_bEnable; }
    
    ComponentType m_eType;
    
protected:
    int SetState(int nAdd, int nRemove);
    
    virtual int OnReceive(CMediaSample& sample);
    
    int     m_nState;
    CLock   m_csState;
    
    BOOL    m_bEOS;
    BOOL    m_bFlush;
    BOOL    m_bEnable;
    
    GUID    m_guid;
    
    vector<CMediaObject*> m_vecInObjs;
    vector<CMediaObject*> m_vecOutObjs;
    
    IReferenceClock*  m_pRefClock;
};

class CSource : public CMediaObject
{
public:
    CSource(const GUID& guid, IDependency* pDepend);
    virtual ~CSource();
};

class CSink : public CMediaObject
{
public:
    CSink(const GUID& guid, IDependency* pDepend);
    virtual ~CSink();
};

typedef enum {
    SAMPLE_RAWDATA = 0,
    SAMPLE_PACKET,
    SAMPLE_PCM,
    SAMPLE_FRAME,
    SAMPLE_MESSAGE,
    SAMPLE_NONE
} SAMPLE_TYPE;

class CMediaSample
{
    friend class CSamplePool;
public:
    CMediaSample();
    ~CMediaSample();
    
    SAMPLE_TYPE     m_Type;
    BYTE*           m_pBuf;
    BYTE*           m_pCur;
    UINT            m_nSize;
    UINT            m_nActual; // might be smaller than m_nSize
    LONGLONG        m_llTimestamp;
    LONGLONG        m_llSyncPoint;
    void*           m_pSpecs;
    UINT            m_nSpecsSize;
    void*           m_pExten;
    UINT            m_nExtenSize;
    BOOL            m_bIgnore;
    BOOL            m_bDiscon;
    CMediaObject*   m_pListener;
    
protected:
    void* GetOwner() const { return m_pOwner; }
    
    void* m_pOwner;
};

#endif
