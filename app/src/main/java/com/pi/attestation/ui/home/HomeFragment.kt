package com.pi.attestation.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.pi.attestation.R
import com.pi.attestation.objects.Certificate
import com.pi.attestation.tools.CertificatesManager
import com.pi.attestation.ui.profile.InfoManager
import com.pi.attestation.ui.tools.ViewModelFactory

/**
 * [Fragment] displayed when we open the app (as "home" [Fragment]). This [Fragment] displays all
 * the previous certificates or a help box in case there is no certificate.
 */
class HomeFragment : Fragment() {

    private lateinit var homeViewModel : HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentActivity = activity ?: return

        homeViewModel = ViewModelProvider(this,
            ViewModelFactory(fragmentActivity.filesDir))
            .get(HomeViewModel::class.java)

        val recyclerView: RecyclerView = view.findViewById(R.id.certificatesRV)
        val infoManager = InfoManager(fragmentActivity)
        val adapter = CertificatesAdapter(infoManager.hasBeenFilled(infoManager.retrieveUserInfo()))
        recyclerView.adapter = adapter
        enableSwipeToDelete(view, recyclerView)

        homeViewModel.certificates.observe(viewLifecycleOwner, Observer {
            adapter.setItems(it)
        })
    }

    /**
     * Enables the swipe to delete feature. With this feature enabled the user can delete a
     * [Certificate] just by swiping to the left. When deleting a [Certificate] a [Snackbar] will be
     * shown to provide a "undo" feature to the user in case it was accidental.
     * @param view [View] where to create the [Snackbar] displayed in case of [Certificate]
     * deletion.
     * @param recyclerView [RecyclerView] where the feature should be enabled.
     * @see SwipeToDeleteCallback
     */
    private fun enableSwipeToDelete(view: View, recyclerView: RecyclerView){
        val swipeToDeleteCallback: SwipeToDeleteCallback =
            object : SwipeToDeleteCallback(view.context) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
                    val position = viewHolder.adapterPosition - 1
                    if(position < 0) return
                    val certificate = homeViewModel.getCertificate(position) ?: return
                    val dirFile = activity?.filesDir
                    if (dirFile != null) {
                        homeViewModel.removeItem(position)
                        CertificatesManager(dirFile).removeCertificate(certificate)
                        Snackbar.make(view, R.string.certificate_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo) {
                                homeViewModel.addItem(certificate, position)
                                recyclerView.scrollToPosition(position)
                                CertificatesManager(dirFile).addCertificate(certificate, position)
                            }.addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    if (event != DISMISS_EVENT_ACTION) {
                                        CertificatesManager(dirFile).deletePdf(certificate)
                                    }
                                }
                            })
                            .show()
                    }else Toast.makeText(view.context, R.string.unknown_error,
                        Toast.LENGTH_SHORT).show()
                }
            }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}
