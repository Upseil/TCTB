@ECHO OFF

SET commands=export-title-commands
SET ids=background-white background-black filling-_s_tay filling-s_t_ay filling-st_a_y filling-sta_y_ filling-_c_olorful filling-c_o_lorful filling-co_l_orful filling-col_o_rful filling-colo_r_ful filling-color_f_ul filling-colorf_u_l filling-colorfu_l_ outlines-stay outlines-colorful
SET DPI=192

IF EXIST %commands% (
	DEL %commands%
)

FOR %%i IN (%ids%) DO (
	ECHO -C -d %DPI% -i %%i -j -e %%i.png title.svg >> %commands%
)
ECHO quit >> %commands%

inkscape --shell < %commands%
DEL %commands%