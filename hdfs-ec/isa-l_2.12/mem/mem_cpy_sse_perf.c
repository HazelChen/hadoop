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

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "mem_routines.h"
#include "test.h"

#define TEST_LEN     (4*1024)
#define TEST_LOOPS   16000000
#define TEST_TYPE_STR "_warm"

int main(int argc, char *argv[])
{
	void *src, *des;
	const int siz = TEST_LEN;
	const int loops = TEST_LOOPS;
	int i;
	struct perf start, stop;

	if ((src = malloc(siz)) == NULL) {
		printf("Error: src mem allocate failed.\n");
		return -1;
	}

	if ((des = malloc(siz)) == NULL) {
		printf("Error: des mem allocate failed.\n");
		return -1;
	}

	memset(src, 0x55, siz);
	memset(des, 0xAA, siz);

	/* Run stdlib memcmp test */
	// Warm up
	memcpy(des, src, siz);

	perf_start(&start);
	for (i = 0; i < loops; i++) {
		*((unsigned char *)des + siz - 1) += 1;
		memcpy(des, src, siz);
	}

	perf_stop(&stop);
	printf("stdlib_memcpy" TEST_TYPE_STR ": ");
	perf_print(stop, start, (long long)siz * loops);

	/* Run mem_cmp_avx performance test */
	// warm up
	mem_cpy_sse(des, src, siz);

	perf_start(&start);
	for (i = 0; i < loops; i++) {
		*((unsigned char *)des + siz - 1) += 1;
		mem_cpy_sse(des, src, siz);
	}

	perf_stop(&stop);
	printf("mem_cpy_sse" TEST_TYPE_STR ":   ");
	perf_print(stop, start, (long long)siz * loops);

	if (memcmp(des, src, siz))
		printf("Some test failed.\n");

	return 0;
}
