//
//  Graph.h
//  QVOD
//
//  Created by bigbug on 11-11-13.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_Graph_h
#define QVOD_Graph_h

#define MAX_V       20

#define NO_EDGE     0
#define HAVE_EDGE   1

#include <cassert>
#include "Utinities.h"

template <class T> 
class CGraph
{
public:
    CGraph() : m_nCntV(0), m_nCntE(0)
    {
        memset(m_Edges, 0, sizeof(m_Edges) / sizeof(int*));
        
        for (int i = 0; i < MAX_V; ++i) {
            memset(m_Verts + i, 0, sizeof(T));
            m_Edges[i] = new int[MAX_V];
            assert(m_Edges[i]);
            for (int j = 0; j < MAX_V; ++j) {
                m_Edges[i][j] = NO_EDGE;
            }
        }
    }
    
    virtual ~CGraph() 
    {
        for (int i = 0; i < MAX_V; ++i) {
            if (m_Edges[i]) {
                delete[] m_Edges[i];
                m_Edges[i] = NULL;
            }
        }
    }
    
    BOOL InsertV(const T& v) 
    {
        if (Find(v)) {
            return FALSE;
        }
        
        m_Verts[m_nCntV] = v;
        for (int i = 0; i < MAX_V; ++i) {
            m_Edges[m_nCntV][i] = NO_EDGE;
        }
        ++m_nCntV;
        
        return TRUE;
    }
    
    BOOL InsertE(T v1, T v2)
    {
        int i, j;
        
        BOOL b1 = Find(v1, i);
        BOOL b2 = Find(v2, j);
        if (!memcpy(&v1, &v2, sizeof(T)) || !b1 || !b2) {
            return FALSE;
        }
        if (m_Edges[i][j] != NO_EDGE) {
            return FALSE;
        }
        m_Edges[i][j] = HAVE_EDGE;
        ++m_nCntE;
        
        return TRUE;
    }
    
    const T& GetV(int n)
    {
        if (n < 0 || n >= MAX_V) {
            assert(0);
        }
        
        return m_Verts[n];
    }
    
    int FirstV(int m)
    {
        for (int i = 0; i < m_nCntV; ++i) {
            if (m_Edges[m][i] != NO_EDGE) {
                return i;
            }
        }
        
        return -1;
    }
    
    int NextV(int m, int n)
    {
        for (int i = n + 1; i < m_nCntV; ++i) {
            if (m_Edges[m][i] != NO_EDGE)
                return i;
        }
        
        return -1;
    }
    
    BOOL Find(const T& v)
    {
        for (int i = 0; i < m_nCntV; ++i) {
            if (!memcmp(&v, &m_Verts[i], sizeof(T))) {
                return TRUE;
            }
        }
        
        return FALSE;
    }
    
    BOOL Find(const T& v, int& i) 
    {
        for (i = 0; i < m_nCntV; ++i) {
            if (!memcmp(&v, &m_Verts[i], sizeof(T))) {
                return TRUE;
            }
        }
        i = -1;
        
        return FALSE;
    }
    
    int Size() const
    {
        return m_nCntV;
    }
    
private:
    int   m_nCntV;
    int   m_nCntE;
    
    T     m_Verts[MAX_V];
    int*  m_Edges[MAX_V];
};

#endif
