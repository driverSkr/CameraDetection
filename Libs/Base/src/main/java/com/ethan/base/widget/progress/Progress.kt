package com.ethan.base.widget.progress

class Progress {
    init {
        throw Exception()
    }

    companion object {
        fun showProgressDialog(support: ProgressSupport?, content: String?) {
            showProgressDialog(support, content, 0, 0, 0, 0)
        }

        fun showProgressDialog(support: ProgressSupport?, content: String?, marginLeft: Int,
                               marginTop: Int, marginRight: Int, marginBottom: Int) {
            if (support == null || support.isDestroy()) {
                return
            }
            support.getUIHandler().post(Runnable {
                if (support.isDestroy()) {
                    return@Runnable
                }
//                showDialog(support, content, marginLeft, marginTop, marginRight, marginBottom)
            })
        }

//        fun dismissProgressDialog(support: ProgressSupport?) {
//            if (support == null || support.isDestroy()) {
//                return
//            }
//            support.getUIHandler().post(Runnable {
//                if (support.isDestroy()) {
//                    return@Runnable
//                }
//                val progressDialog = support.getProgressDialog()
//                if (progressDialog != null && progressDialog.isShowing) {
//                    // Dialog相关操作加上异常保护,避免外部Activity的Window被销毁后执行显示,产生异常.
//                    try {
//                        progressDialog.dismiss()
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//            })
//        }
//
//        private fun showDialog(support: ProgressSupport, content: String?, marginLeft: Int,
//                               marginTop: Int, marginRight: Int, marginBottom: Int) {
//            val progressDialog = support.getProgressDialog() ?: return
//            if (marginLeft != 0 || marginTop != 0 || marginRight != 0 || marginBottom != 0) {
//                progressDialog.resetContentViewMargin(marginLeft, marginTop, marginRight, marginBottom)
//            }
//            // Dialog显示相关操作加上异常保护,避免外部Activity的Window被销毁后执行显示,产生异常.
//            try {
//                if (progressDialog.isShowing) {
//                    progressDialog.setMessage(content)
//                } else {
//                    progressDialog.setMessage(content)
//                    progressDialog.show()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
    }
}