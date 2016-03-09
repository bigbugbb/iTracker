//
//  FFmpegCallbacks.h
//  QVOD
//
//  Created by bigbug on 11-11-16.
//  Copyright (c) 2011年 qvod. All rights reserved.
//

#ifndef QVOD_FFmpegCallbacks_h
#define QVOD_FFmpegCallbacks_h

int avio_interrupt_cb();
void maintain_avio();
void interrupt_avio();

int avio_is_remote();
void avio_set_remote(int nRemote);

int avcodec_is_loop_filter();
void avcodec_enable_loop_filter(int nLoopFilter);

int notify_seek_pos_cb(int64_t, void*);
int notify_recv_size_cb(int64_t, void*);
int notify_buf_size_cb(int64_t, void*);
int notify_read_index_cb(int64_t, void*);
int notify_reconnect_cb(int64_t, void*);

#endif
