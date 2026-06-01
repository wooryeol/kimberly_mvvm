package kr.co.kimberly.wma.network.model

import java.io.Serializable

data class ResultValuesModel(var rtnCode: String,
                             var rtnMsg: String,
                             var ev_batch: String,     //ZWM03C_PDA_IN_0014
                             var plt_no: String,    //ZWM03C_PDA_IN_0070, ZWM03C_PDA_IN_0071
                             var plt_no2: String,    //ZWM03C_PDA_IN_0071
                             var tuext: String,     //ZWM03C_PDA_IN_0057
                             var accar: String,      //ZWM03C_PDA_IN_0057
                             var matnr: String,     //ZWM03C_PDA_OUT_0070_SUM
                             var maktx: String,     //ZWM03C_PDA_OUT_0070_SUM
                             var tot_qty: String,     //ZWM03C_PDA_OUT_0070_SUM
                             var topkqty: String,     //ZWM03C_PDA_OUT_0075
                             var toipqty: String     //ZWM03C_PDA_OUT_0075
): Serializable