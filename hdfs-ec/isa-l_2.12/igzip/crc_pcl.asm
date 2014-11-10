;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;  Copyright(c) 2011-2014 Intel Corporation All rights reserved.
;
;  Redistribution and use in source and binary forms, with or without
;  modification, are permitted provided that the following conditions 
;  are met:
;    * Redistributions of source code must retain the above copyright
;      notice, this list of conditions and the following disclaimer.
;    * Redistributions in binary form must reproduce the above copyright
;      notice, this list of conditions and the following disclaimer in
;      the documentation and/or other materials provided with the
;      distribution.
;    * Neither the name of Intel Corporation nor the names of its
;      contributors may be used to endorse or promote products derived
;      from this software without specific prior written permission.
;
;  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
;  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
;  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
;  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
;  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
;  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
;  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
;  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
;  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
;  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

%include "options.asm"

;; For debugging purposes
%ifdef _DEBUG
%define movntdqa movdqa
%define pextrd pextrw
%macro pclmulqdq 3
	pxor	%1, %2
%endm
%endif

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; code for doing the CRC calculation as part of copy-in, using pclmulqdq

; "shift" 4 input registers down 4 places 
; macro FOLD4	xmm0, xmm1, xmm2, xmm3, const, tmp0, tmp1
%macro FOLD4	7
%define %%xmm0	%1	; xmm reg, in/out
%define %%xmm1	%2	; xmm reg, in/out
%define %%xmm2	%3	; xmm reg, in/out
%define %%xmm3	%4	; xmm reg, in/out
%define %%const	%5	; xmm reg, in
%define %%tmp0	%6	; xmm reg, tmp
%define %%tmp1	%7	; xmm reg, tmp

	movaps		%%tmp0, %%xmm0
	movaps		%%tmp1, %%xmm1

	pclmulqdq	%%xmm0, %%const, 0x01
	pclmulqdq	%%xmm1, %%const, 0x01

	pclmulqdq	%%tmp0, %%const, 0x10
	pclmulqdq	%%tmp1, %%const, 0x10

	xorps		%%xmm0, %%tmp0
	xorps		%%xmm1, %%tmp1


	movaps		%%tmp0, %%xmm2
	movaps		%%tmp1, %%xmm3

	pclmulqdq	%%xmm2, %%const, 0x01
	pclmulqdq	%%xmm3, %%const, 0x01

	pclmulqdq	%%tmp0, %%const, 0x10
	pclmulqdq	%%tmp1, %%const, 0x10

	xorps		%%xmm2, %%tmp0
	xorps		%%xmm3, %%tmp1
%endm

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; "shift" 3 input registers down 4 places 
; macro FOLD3	x0, x1, x2, x3, const, tmp0
;     x0 x1 x2 x3
; In   A  B  C  D
; Out  D  A' B' C'
%macro FOLD3	6
%define %%x0	%1	; xmm reg, in/out
%define %%x1	%2	; xmm reg, in/out
%define %%x2	%3	; xmm reg, in/out
%define %%x3	%4	; xmm reg, in/out
%define %%const	%5	; xmm reg, in
%define %%tmp0	%6	; xmm reg, tmp

	movdqa		%%tmp0, %%x3

	movaps		%%x3, %%x2
	pclmulqdq	%%x2, %%const, 0x01
	pclmulqdq	%%x3, %%const, 0x10
	xorps		%%x3, %%x2

	movaps		%%x2, %%x1
	pclmulqdq	%%x1, %%const, 0x01
	pclmulqdq	%%x2, %%const, 0x10
	xorps		%%x2, %%x1

	movaps		%%x1, %%x0
	pclmulqdq	%%x0, %%const, 0x01
	pclmulqdq	%%x1, %%const, 0x10
	xorps		%%x1, %%x0

	movdqa		%%x0, %%tmp0
%endm

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; "shift" 2 input registers down 4 places 
; macro FOLD2	x0, x1, x2, x3, const, tmp0
;     x0 x1 x2 x3
; In   A  B  C  D
; Out  C  D  A' B'
%macro FOLD2	6
%define %%x0	%1	; xmm reg, in/out
%define %%x1	%2	; xmm reg, in/out
%define %%x2	%3	; xmm reg, in/out
%define %%x3	%4	; xmm reg, in/out
%define %%const	%5	; xmm reg, in
%define %%tmp0	%6	; xmm reg, tmp

	movdqa		%%tmp0, %%x3

	movaps		%%x3, %%x1
	pclmulqdq	%%x1, %%const, 0x01
	pclmulqdq	%%x3, %%const, 0x10
	xorps		%%x3, %%x1

	movdqa		%%x1, %%tmp0
	movdqa		%%tmp0, %%x2

	movaps		%%x2, %%x0
	pclmulqdq	%%x0, %%const, 0x01
	pclmulqdq	%%x2, %%const, 0x10
	xorps		%%x2, %%x0

	movdqa		%%x0, %%tmp0
%endm

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; "shift" 1 input registers down 4 places 
; macro FOLD1	x0, x1, x2, x3, const, tmp0
;     x0 x1 x2 x3
; In   A  B  C  D
; Out  B  C  D  A'
%macro FOLD1	6
%define %%x0	%1	; xmm reg, in/out
%define %%x1	%2	; xmm reg, in/out
%define %%x2	%3	; xmm reg, in/out
%define %%x3	%4	; xmm reg, in/out
%define %%const	%5	; xmm reg, in
%define %%tmp0	%6	; xmm reg, tmp

	movdqa		%%tmp0, %%x3

	movaps		%%x3, %%x0
	pclmulqdq	%%x0, %%const, 0x01
	pclmulqdq	%%x3, %%const, 0x10
	xorps		%%x3, %%x0

	movdqa		%%x0, %%x1
	movdqa		%%x1, %%x2
	movdqa		%%x2, %%tmp0
%endm

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; macro PARTIAL_FOLD x0, x1, x2, x3, xp, size, xfold, xt0, xt1, xt2, xt3
	
;                  XP X3 X2 X1 X0 tmp2
; Initial state    xI HG FE DC BA
; after shift         IH GF ED CB A0
; after fold          ff GF ED CB      ff = merge(IH, A0)
; 
%macro PARTIAL_FOLD 12
%define %%x0	%1	; xmm reg, in/out
%define %%x1	%2	; xmm reg, in/out
%define %%x2	%3	; xmm reg, in/out
%define %%x3	%4	; xmm reg, in/out
%define %%xp    %5	; xmm partial reg, in/clobbered
%define %%size  %6      ; GPR,     in/clobbered  (1...15)
%define %%const	%7	; xmm reg, in
%define %%shl	%8	; xmm reg, tmp
%define %%shr	%9	; xmm reg, tmp
%define %%tmp2	%10	; xmm reg, tmp
%define %%tmp3	%11	; xmm reg, tmp
%define %%gtmp  %12	; GPR,     tmp

	; {XP X3 X2 X1 X0} = {xI HG FE DC BA}
	shl	%%size, 4	; size *= 16
	lea	%%gtmp, [pshufb_shf_table - 16 wrt rip WRT_OPT]
	movdqa	%%shl, [%%gtmp + %%size]		; shl constant
	movdqa	%%shr, %%shl
	pxor %%shr, [mask3 wrt rip WRT_OPT]		; shr constant

	movdqa	%%tmp2, %%x0	; tmp2 = BA
	pshufb	%%tmp2, %%shl	; tmp2 = A0
	
	pshufb	%%x0, %%shr	; x0 = 0B
	movdqa	%%tmp3, %%x1	; tmp3 = DC
	pshufb	%%tmp3, %%shl	; tmp3 = C0
	por	%%x0, %%tmp3	; x0 = CB

	pshufb	%%x1, %%shr	; x1 = 0D
	movdqa	%%tmp3, %%x2	; tmp3 = FE
	pshufb	%%tmp3, %%shl	; tmp3 = E0
	por	%%x1, %%tmp3	; x1 = ED

	pshufb	%%x2, %%shr	; x2 = 0F
	movdqa	%%tmp3, %%x3	; tmp3 = HG
	pshufb	%%tmp3, %%shl	; tmp3 = G0
	por	%%x2, %%tmp3	; x2 = GF

	pshufb	%%x3, %%shr	; x3 = 0H
	pshufb	%%xp, %%shl	; xp = I0
	por	%%x3, %%xp	; x3 = IH

	; fold tmp2 into X3
	movaps		%%tmp3, %%tmp2
	pclmulqdq	%%tmp2, %%const, 0x01
	pclmulqdq	%%tmp3, %%const, 0x10
	xorps		%%x3, %%tmp2
	xorps		%%x3, %%tmp3
%endm

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; copy x bytes (rounded up to 16 bytes) from src to dst
; src & dst are unaligned
; macro COPY_IN_CRC dst, src, size_in_bytes, tmp, x0, x1, x2, x3, xfold, 
;		    xt0, xt1, xt2, xt3, xt4
%macro COPY_IN_CRC 14
%define %%dst	%1	; reg, in/clobbered
%define %%src	%2	; reg, in/clobbered
%define %%size	%3	; reg, in/clobbered
%define %%tmp	%4	; reg, tmp
%define %%x0	%5	; xmm, in/out: crc state
%define %%x1	%6	; xmm, in/out: crc state
%define %%x2	%7	; xmm, in/out: crc state
%define %%x3	%8	; xmm, in/out: crc state
%define %%xfold %9	; xmm, in:	(loaded from fold4)
%define %%xtmp0	%10	; xmm, tmp
%define %%xtmp1	%11	; xmm, tmp
%define %%xtmp2	%12	; xmm, tmp
%define %%xtmp3	%13	; xmm, tmp
%define %%xtmp4	%14	; xmm, tmp

	cmp	%%size, 16
	jl	%%lt_16

	; align source
	xor	%%tmp, %%tmp
	sub	%%tmp, %%src
	and	%%tmp, 15
	jz	%%already_aligned

	; need to align, tmp contains number of bytes to transfer
	movdqu	%%xtmp0, [%%src]
	movdqu	[%%dst], %%xtmp0
	add	%%dst, %%tmp
	add	%%src, %%tmp
	sub	%%size, %%tmp

%ifndef DEFLATE
	push	%%dst

	PARTIAL_FOLD	%%x0, %%x1, %%x2, %%x3, %%xtmp0, %%tmp, %%xfold, \
			%%xtmp1, %%xtmp2, %%xtmp3, %%xtmp4, %%dst
	pop	%%dst
%endif

%%already_aligned:
	sub	%%size, 64
	jl	%%end_loop
	jmp	%%loop
align 16
%%loop:
	movntdqa	%%xtmp0, [%%src+0*16]
	movntdqa	%%xtmp1, [%%src+1*16]
	movntdqa	%%xtmp2, [%%src+2*16]

%ifndef DEFLATE
	FOLD4		%%x0, %%x1, %%x2, %%x3, %%xfold, %%xtmp3, %%xtmp4
%endif	
	movntdqa	%%xtmp3, [%%src+3*16]

	movdqu		[%%dst+0*16], %%xtmp0
	movdqu		[%%dst+1*16], %%xtmp1
	movdqu		[%%dst+2*16], %%xtmp2
	movdqu		[%%dst+3*16], %%xtmp3

%ifndef DEFLATE
	pxor		%%x0, %%xtmp0
	pxor		%%x1, %%xtmp1
	pxor		%%x2, %%xtmp2
	pxor		%%x3, %%xtmp3
%endif
	add	%%src,  4*16
	add	%%dst,  4*16
	sub	%%size, 4*16
	jge	%%loop

%%end_loop:
	; %%size contains (num bytes left - 64)
	add	%%size, 16
	jge	%%three_full_regs
	add	%%size, 16
	jge	%%two_full_regs
	add	%%size, 16
	jge	%%one_full_reg
	add	%%size, 16

%%no_full_regs:		; 0 <= %%size < 16, no full regs
	jz		%%done	; if no bytes left, we're done
	movntdqa	%%xtmp0, [%%src]
	jmp		%%partial

	;; Handle case where input is <16 bytes
%%lt_16:
	test		%%size, %%size
	jz		%%done	; if no bytes left, we're done
	movdqu		%%xtmp0, [%%src]
	jmp		%%partial


%%one_full_reg:
	movntdqa	%%xtmp0, [%%src+0*16]

%ifndef DEFLATE
	FOLD1		%%x0, %%x1, %%x2, %%x3, %%xfold, %%xtmp3
%endif
	movdqu		[%%dst+0*16], %%xtmp0

%ifndef DEFLATE
	pxor		%%x3, %%xtmp0
%endif
	test		%%size, %%size
	jz		%%done	; if no bytes left, we're done
	add		%%dst, 1*16
	movntdqa	%%xtmp0, [%%src+1*16]
	jmp		%%partial


%%two_full_regs:
	movntdqa	%%xtmp0, [%%src+0*16]
	movntdqa	%%xtmp1, [%%src+1*16]

%ifndef DEFLATE
	FOLD2		%%x0, %%x1, %%x2, %%x3, %%xfold, %%xtmp3
%endif
	movdqu		[%%dst+0*16], %%xtmp0
	movdqu		[%%dst+1*16], %%xtmp1

%ifndef DEFLATE
	pxor		%%x2, %%xtmp0
	pxor		%%x3, %%xtmp1
%endif
	test		%%size, %%size
	jz		%%done	; if no bytes left, we're done
	add		%%dst, 2*16
	movntdqa	%%xtmp0, [%%src+2*16]
	jmp		%%partial


%%three_full_regs:
	movntdqa	%%xtmp0, [%%src+0*16]
	movntdqa	%%xtmp1, [%%src+1*16]
	movntdqa	%%xtmp2, [%%src+2*16]

%ifndef DEFLATE
	FOLD3		%%x0, %%x1, %%x2, %%x3, %%xfold, %%xtmp3
%endif
	movdqu		[%%dst+0*16], %%xtmp0
	movdqu		[%%dst+1*16], %%xtmp1
	movdqu		[%%dst+2*16], %%xtmp2

%ifndef DEFLATE
	pxor		%%x1, %%xtmp0
	pxor		%%x2, %%xtmp1
	pxor		%%x3, %%xtmp2
%endif
	test		%%size, %%size
	jz		%%done	; if no bytes left, we're done
	add		%%dst, 3*16
	movntdqa	%%xtmp0, [%%src+3*16]
	; fall through to %%partial


%%partial:		; 0 <= %%size < 16
	movdqu		[%%dst], %%xtmp0

%ifndef DEFLATE
	PARTIAL_FOLD	%%x0, %%x1, %%x2, %%x3, %%xtmp0, %%size, %%xfold, \
			%%xtmp1, %%xtmp2, %%xtmp3, %%xtmp4, %%dst
%endif

%%done:
%endm

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

%ifndef NO_CRC_DATA_EXTERNS

extern pshufb_shf_table
extern mask3
extern fold_4
extern rk1
extern rk5
extern rk7
extern mask
extern mask2
extern mask3

%endif
