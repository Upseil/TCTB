@ECHO OFF

SET dir=%1
IF "%1"=="" (
	SET dir=.
)

SET commands=export-title-commands
SET extracted=%dir%\extracted

SET DPI=196

REM Letters with outlines
SET paged_ids=background-white background-black filling-_s_tay filling-s_t_ay filling-st_a_y filling-sta_y_ filling-_c_olorful filling-c_o_lorful filling-co_l_orful filling-col_o_rful filling-colo_r_ful filling-color_f_ul filling-colorf_u_l filling-colorfu_l_ outlines-stay outlines-colorful

REM Letters with shadows
REM SET paged_ids=background-white background-black filling-_s_tay filling-s_t_ay filling-st_a_y filling-sta_y_ filling-_c_olorful filling-c_o_lorful filling-co_l_orful filling-col_o_rful filling-colo_r_ful filling-color_f_ul filling-colorf_u_l filling-colorfu_l_ shadow-_s_tay shadow-s_t_ay shadow-st_a_y shadow-sta_y_ shadow-_c_olorful shadow-c_o_lorful shadow-co_l_orful shadow-col_o_rful shadow-colo_r_ful shadow-color_f_ul shadow-colorf_u_l shadow-colorfu_l_

REM Blured border of the background
SET drawing_ids=background-blur

IF EXIST %commands% (
	DEL %commands%
)
REM Clean work directory from old image files
FORFILES /p "%extracted%" /m *.png /c "cmd /c DEL @path"

FOR %%i IN (%paged_ids%) DO (
	ECHO -C -d %DPI% -i %%i -j -e "%extracted%\%%i.png" "%dir%\title.svg" >> %commands%
)
FOR %%i IN (%drawing_ids%) DO (
	ECHO -D -d %DPI% -i %%i -j -e "%extracted%\%%i.png" "%dir%\title.svg" >> %commands%
)
ECHO quit >> %commands%

inkscape --shell < %commands%
DEL %commands%