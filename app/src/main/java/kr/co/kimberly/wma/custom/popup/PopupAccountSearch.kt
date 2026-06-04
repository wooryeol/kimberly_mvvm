package kr.co.kimberly.wma.custom.popup

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import kr.co.kimberly.wma.GlobalApplication
import kr.co.kimberly.wma.adapter.AccountSearchAdapter
import kr.co.kimberly.wma.common.Define
import kr.co.kimberly.wma.common.Utils
import kr.co.kimberly.wma.custom.OnSingleClickListener
import kr.co.kimberly.wma.databinding.PopupAccountSearchBinding
import kr.co.kimberly.wma.network.ApiClientService
import kr.co.kimberly.wma.network.model.CustomerModel
import kr.co.kimberly.wma.network.model.login.LoginResponse
import kr.co.kimberly.wma.network.model.ResultModel
import retrofit2.Call
import retrofit2.Response

@SuppressLint("NotifyDataSetChanged")
class PopupAccountSearch(mContext: Context): Dialog(mContext) {
    private lateinit var mBinding: PopupAccountSearchBinding
    private var mLoginInfo: LoginResponse? = null // 로그인 정보
    private var context = mContext

    var onItemSelect: ((CustomerModel) -> Unit)? = null
    var list : List<CustomerModel>? = null
    var adapter: AccountSearchAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = PopupAccountSearchBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        initViews()
    }

    private fun initViews() {
        mLoginInfo = Utils.getLoginData()

        // setCancelable(false) // 뒤로가기 버튼, 바깥 화면 터치시 닫히지 않게

        // (중요) Dialog 는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius 가 보이지 않음
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        adapter = AccountSearchAdapter(context)
        mBinding.recyclerview.adapter = adapter
        mBinding.recyclerview.layoutManager = LinearLayoutManager(context)

        mBinding.btLogin.setOnClickListener(object : OnSingleClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSingleClick(v: View) {
                if (mBinding.etAccount.text.isNullOrEmpty()) {
                    Utils.popupNotice(context, "거래처를 입력해주세요")
                } else {
                    searchCustomer() // 거래처 검색 통신
                    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(mBinding.etAccount.windowToken, 0)
                }
            }
        })

        mBinding.etAccount.addTextChangedListener {
            if (mBinding.etAccount.text.isNullOrEmpty()) {
                mBinding.btProductNameEmpty.visibility = View.GONE
            } else {
                mBinding.btProductNameEmpty.visibility = View.VISIBLE
            }
        }

        mBinding.btProductNameEmpty.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                mBinding.etAccount.text = null
                adapter?.dataList = emptyList()
                mBinding.noSearch.visibility = View.GONE
                mBinding.recyclerview.visibility = View.VISIBLE
                adapter?.notifyDataSetChanged()
                GlobalApplication.showKeyboard(context, mBinding.etAccount)
            }
        })

        adapter?.itemClickListener = object: AccountSearchAdapter.ItemClickListener {
            override fun onItemClick(item: CustomerModel) {
                onItemSelect?.invoke(item)
                hideDialog()
            }
        }

        mBinding.btnClose.setOnClickListener(object : OnSingleClickListener(){
            override fun onSingleClick(v: View) {
                hideDialog()
            }
        })

        mBinding.etAccount.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                mBinding.btLogin.performClick()
                true
            } else {
                false
            }
        }
    }

    fun hideDialog() {
        if (isShowing) {
            dismiss()
        }
    }

    private fun searchCustomer() {
        val loading = PopupLoading(context)
        loading.show()
        val retrofit = ApiClientService.ApiClient.getLoginRetrofit()
        val service = retrofit.create(ApiClientService::class.java)
        val searchCondition = mBinding.etAccount.text.toString()
        val call = service.client(mLoginInfo?.agencyCd!!, mLoginInfo?.userId!!, searchCondition)

        call.enqueue(object : retrofit2.Callback<ResultModel<List<CustomerModel>>> {
            override fun onResponse(
                call: Call<ResultModel<List<CustomerModel>>>,
                response: Response<ResultModel<List<CustomerModel>>>
            ) {
                loading.hideDialog()
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item?.returnCd == Define.RETURN_CD_00 || item?.returnCd == Define.RETURN_CD_90 || item?.returnCd == Define.RETURN_CD_91) {
                        // Utils.log("account search success ====> ${Gson().toJson(item)}")
                        list = item.data as ArrayList<CustomerModel>
                        adapter?.dataList = list!!
                        adapter?.notifyDataSetChanged()

                        if (list.isNullOrEmpty()) {
                            mBinding.recyclerview.visibility = View.GONE
                            mBinding.noSearch.visibility = View.VISIBLE
                        } else {
                            mBinding.recyclerview.visibility = View.VISIBLE
                            mBinding.noSearch.visibility = View.GONE
                        }
                    } else {
                        Utils.popupNotice(context, item?.returnMsg!!, mBinding.etAccount)
                    }
                } else {
                    // Utils.log("${response.code()} ====> ${response.message()}")
                    Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
                }
            }

            override fun onFailure(call: Call<ResultModel<List<CustomerModel>>>, t: Throwable) {
                loading.hideDialog()
                // Utils.log("search failed ====> ${t.message}")
                Utils.popupNotice(context, "잠시 후 다시 시도해주세요")
            }

        })
    }

}