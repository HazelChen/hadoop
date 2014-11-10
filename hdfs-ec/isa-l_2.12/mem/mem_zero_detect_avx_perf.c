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
#include <stdlib.h>
#include "mem_routines.h"
#include "test.h"

#define TEST_LEN     8*1024
#define TEST_LOOPS   10000000
#define TEST_TYPE_STR "_warm"

int mem_zero_detect_ref(void *buf, int n)
{
	unsigned char *c;
#if __WORDSIZE == 64
	unsigned long long a = 0, *p = buf;
#else
	unsigned int a = 0, *p = buf;
#endif

	// Check buffer in native machine width comparisons
	while (0 <= (n -= sizeof(p))) {
		if (*p++ != 0)
			return -1;
	}

	// Check remaining bytes
	c = (unsigned char *)p;

	switch (sizeof(p) + n) {
	case 7:
		a |= *c++;	// fall through to case 6,5,4
	case 6:
		a |= *c++;	// fall through to case 5,4
	case 5:
		a |= *c++;	// fall through to case 4
	case 4:
		a |= *((unsigned int *)c);
		break;
	case 3:
		a |= *c++;	// fall through to case 2
	case 2:
		a |= *((unsigned short *)c);
		break;
	case 1:
		a |= *c;
		break;
	}

	return (a == 0) ? 0 : -1;
}

int main(int argc, char *argv[])
{
	int i;
	int val = 0;
	void *buf;
	struct perf start, stop;

	printf("Test mem_zero_detect_avx_perf %d bytes\n", TEST_LEN);

	if (posix_memalign(&buf, 64, TEST_LEN)) {
		printf("alloc error: Fail");
		return -1;
	}
#ifdef DO_REF_PERF
	// Warm up
	mem_zero_detect_ref(buf, TEST_LEN);

	perf_start(&start);

	for (i = 0; i < TEST_LOOPS; i++)
		val |= mem_zero_detect_ref(buf, TEST_LEN);

	perf_stop(&stop);
	printf("mem_zero_detect_ref" TEST_TYPE_STR ": ");
	perf_print(stop, start, (long long)TEST_LEN * i);
#endif

	// Warm up
	mem_zero_detect_avx(buf, TEST_LEN);

	perf_start(&start);

	for (i = 0; i < TEST_LOOPS; i++)
		val |= mem_zero_detect_avx(buf, TEST_LEN);

	perf_stop(&stop);
	printf("mem_zero_detect_avx" TEST_TYPE_STR ": ");
	perf_print(stop, start, (long long)TEST_LEN * i);

	return 0;
}
