/**********************************************************************
  Copyright(c) 2011-2014 Intel Corporation All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions 
  are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.
    * Neither the name of Intel Corporation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**********************************************************************/

#include <string.h>
#include "mb_md5.h"

void md5_init_mb_mgr(MD5_MB_MGR * state)
{
	unsigned int j;

	state->lens[0] = 0;
	state->lens[1] = 1;
	state->lens[2] = 2;
	state->lens[3] = 3;
	state->lens[4] = 4;
	state->lens[5] = 5;
	state->lens[6] = 6;
	state->lens[7] = 7;
	state->unused_lanes = 0xF76543210;
	for (j = 0; j < 8; j++) {
		state->ldata[j].job_in_lane = (void *)0;
		state->ldata[j].extra_block[64] = 0x80;
		memset(state->ldata[j].extra_block + 65, 0x00, 64 + 7);
	}
}

void md5_init_mb_mgr_x8x2(MD5_MB_MGR_X8X2 * state)
{
	unsigned int j;

	for (j = 0; j < NUM_MD5_LANES_X8X2; j++)
		state->lens[j] = 0;

	/* Load lane indices with one nibble each */
	state->unused_lanes = 0xFEDCBA9876543210;
	/* Initialise our variable for tracking lanes */
	state->num_lanes_inuse = 0;
	for (j = 0; j < NUM_MD5_LANES_X8X2; j++) {
		state->ldata[j].job_in_lane = (void *)0;
		/* Initialise padding block values with 1 followed by zeroes */
		state->ldata[j].extra_block[64] = 0x80;
		memset(state->ldata[j].extra_block + 65, 0x00, 64 + 7);
	}
}

struct slver {
	UINT16 snum;
	UINT8 ver;
	UINT8 core;
};

struct slver md5_init_mb_mgr_slver_00020026;
struct slver md5_init_mb_mgr_slver = { 0x0026, 0x02, 0x00 };

struct slver md5_init_mb_mgr_x8x2_slver_04010106;
struct slver md5_init_mb_mgr_x8x2_slver = { 0x0106, 0x01, 0x04 };
