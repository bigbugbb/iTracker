//
//  MediaGraph.h
//  QVOD
//
//  Created by bigbug on 11-11-13.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_MediaGraph_h
#define QVOD_MediaGraph_h

#include "Graph.h"
#include "DependencyObject.h"
#include <map>
using std::map;

class CMediaObject;

class CMediaGraph : public CDependencyObject
{
public:
    CMediaGraph(int* pResult = NULL);
    virtual ~CMediaGraph();
    
    int  GetSize() const;
    CMediaObject* GetSink() const;
    CMediaObject* GetSource() const;
    BOOL Insert(CMediaObject* pObj);
    BOOL Connect(CMediaObject* pLeftObj, CMediaObject* pRightObj);
    BOOL Prepare(); // used to locate the sink component
    CMediaObject* Find(const GUID& guid);

    // using BFS
    BOOL Traversal(CMediaObject* pObj, int nCommand, void* pParam, int& nResult);
    
protected:
    CMediaObject*  m_pSource;
    CMediaObject*  m_pSink;
    
    CGraph<GUID>*  m_pGraph;
    map<GUID, CMediaObject*> m_mapObjs;
};

#endif
