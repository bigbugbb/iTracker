package com.localytics.android.itracker.ui;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.localytics.android.itracker.R;
import com.localytics.android.itracker.data.model.User;
import com.localytics.android.itracker.util.AccountUtils;

import static com.localytics.android.itracker.util.LogUtils.LOGD;
import static com.localytics.android.itracker.util.LogUtils.makeLogTag;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SignUpFragment.OnAccountSignUpListener} interface
 * to handle interaction events.
 */
public class SignUpFragment extends Fragment {

    private static final String TAG = makeLogTag(SignUpFragment.class);

    private static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    private OnAccountSignUpListener mListener;

    private Button   mSignUp;
    private TextView mAccountExists;
    private EditText mEmail;
    private EditText mPassword;
    private EditText mPasswordConfirmation;

    public SignUpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     *
     * @param view               The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmail = (EditText) view.findViewById(R.id.email);
        mPassword = (EditText) view.findViewById(R.id.password);
        mPasswordConfirmation = (EditText) view.findViewById(R.id.password_confirmation);

        mSignUp = (Button) view.findViewById(R.id.sign_up);
        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEmail.setEnabled(false);
                mPassword.setEnabled(false);
                mPasswordConfirmation.setEnabled(false);
                mSignUp.setEnabled(false);
                mSignUp.setText(R.string.creating_new_account);
                mAccountExists.setEnabled(false);
                mListener.onAccountStartSignUp();

                signUp();
            }
        });
        mAccountExists = (TextView) view.findViewById(R.id.account_exists);
        mAccountExists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onAccountAreadyExists();
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAccountSignUpListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAccountSignUpListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void signUp() {
        // Validation!
        final String email = mEmail.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        final String passwordConfirmation = mPasswordConfirmation.getText().toString().trim();
        final String accountType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_ACCOUNT_TYPE);

        new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {
                LOGD(TAG, "started authenticating");

                Bundle data = new Bundle();
                try {
                    User user = AccountUtils.signUpUser(email, password, passwordConfirmation);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, email);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_PASSWORD, password);
                    data.putString(AccountManager.KEY_AUTHTOKEN, user != null ? user.auth_token : null);

                    Bundle userData = new Bundle();
                    userData.putString(AccountUtils.USERDATA_USER_ID, user != null ? user.id : null);
                    data.putBundle(AccountManager.KEY_USERDATA, userData);
                } catch (Exception e) {
                    data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                }

                final Intent intent = new Intent();
                intent.putExtras(data);
                return intent;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    mListener.onAccountSignUpError(intent.getStringExtra(KEY_ERROR_MESSAGE));
                    mEmail.setEnabled(true);
                    mPassword.setEnabled(true);
                    mPasswordConfirmation.setEnabled(true);
                    mSignUp.setEnabled(true);
                    mSignUp.setText(R.string.sign_up);
                    mAccountExists.setEnabled(true);
                } else {
                    mListener.onAccountSignUpSuccess(intent);
                }
            }
        }.execute();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnAccountSignUpListener {
        void onAccountSignUpSuccess(Intent intent);
        void onAccountSignUpError(String message);
        void onAccountStartSignUp();
        void onAccountAreadyExists();
    }
}
