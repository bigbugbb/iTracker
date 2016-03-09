//
//  Error.cpp
//  QVOD
//
//  Created by bigbug on 11-11-12.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#include <iostream>

#include "Error.h"

//namespace ios_qvod_player
//{

err_t GetLastError()
{
#ifdef WIN32
    
	return GetLastError();
    
#else /* posix */
    
	return errno;
    
#endif
}
    
//} /* end of namespace ios_qvod_player */

