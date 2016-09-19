package com.itracker.android.ui.fragment;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.itracker.android.R;
import com.itracker.android.data.model.User;
import com.itracker.android.ui.activity.AuthenticatorActivity;
import com.itracker.android.utils.AccountUtils;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SignInFragment.OnAccountSignInListener} interface
 * to handle interaction events.
 */
public class SignInFragment extends Fragment {

    private static final String TAG = makeLogTag(SignInFragment.class);

    private static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    private OnAccountSignInListener mListener;

    private Button mSignIn;
    private TextView mNewAccount;
    private EditText mEmail;
    private EditText mPassword;
    private TextInputLayout mEmailInputLayout;
    private TextInputLayout mPasswordInputLayout;

    public SignInFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
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

        mEmail = (EditText) view.findViewById(R.id.edit_email);
        mPassword = (EditText) view.findViewById(R.id.edit_password);

        mEmailInputLayout = (TextInputLayout) view.findViewById(R.id.input_email);
        mPasswordInputLayout = (TextInputLayout) view.findViewById(R.id.input_password);

        mEmail.addTextChangedListener(new AuthTextWatcher(mEmail));
        mPassword.addTextChangedListener(new AuthTextWatcher(mPassword));

        mSignIn = (Button) view.findViewById(R.id.sign_in);
        mSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableUi(false);
                if (mListener != null) {
                    mListener.onAccountStartSignIn();
                }
                signIn();
            }
        });
        mNewAccount = (TextView) view.findViewById(R.id.new_account);
        mNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onAccountWillCreateNew();
                }
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAccountSignInListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAccountSignInListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void signIn() {
        final String email = mEmail.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        final String accountType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_ACCOUNT_TYPE);

        if (!validateEmail(email) || !validatePassword(password)) {
            enableUi(true);
            if (mListener != null) {
                mListener.onAccountSignInError("Invalid input, please try again");
            }
            return;
        }

        new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {
                LOGD(TAG, "Started authenticating");

                Bundle data = new Bundle();
                try {
                    User user = AccountUtils.signInUser(email, password);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, email);
                    data.putString(AuthenticatorActivity.USERNAME, user.username);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_PASSWORD, password);
                    data.putString(AccountManager.KEY_AUTHTOKEN, user.auth_token);

                    Bundle userData = new Bundle();
                    userData.putString(AccountUtils.USERDATA_USER_ID, user != null ? user.id : null);
                    data.putBundle(AccountManager.KEY_USERDATA, userData);
                } catch (Exception e) {
                    data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                }

                final Intent intent = new Intent();
                intent.putExtra(AuthenticatorActivity.CREATE_ACCOUNT, false);
                intent.putExtras(data);
                return intent;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    if (mListener != null) {
                        mListener.onAccountSignInError(intent.getStringExtra(KEY_ERROR_MESSAGE));
                    }
                    enableUi(true);
                } else {
                    if (mListener != null) {
                        mListener.onAccountSignInSuccess(intent);
                    }
                }
            }
        }.execute();
    }

    private void enableUi(boolean enabled) {
        mEmailInputLayout.setEnabled(enabled);
        mPasswordInputLayout.setEnabled(enabled);
        mSignIn.setEnabled(enabled);
        mSignIn.setText(enabled ? R.string.sign_in : R.string.sign_in_user);
        mNewAccount.setEnabled(enabled);
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
            mEmailInputLayout.setError(getString(R.string.invalid_email));
            requestFocus(mEmail);
            return false;
        } else {
            mEmailInputLayout.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            mPasswordInputLayout.setError(getString(R.string.invalid_password));
            requestFocus(mPassword);
            return false;
        } else {
            mPasswordInputLayout.setErrorEnabled(false);
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void requestFocus(View view) {
        if (isAdded() && view.requestFocus()) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private class AuthTextWatcher implements TextWatcher {

        private View mView;

        private AuthTextWatcher(View view) {
            mView = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (mView.getId()) {
                case R.id.input_email:
                    validateEmail(editable.toString().trim());
                    break;
                case R.id.input_password:
                    validatePassword(editable.toString().trim());
                    break;
            }
        }
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
    public interface OnAccountSignInListener {
        void onAccountSignInSuccess(Intent intent);
        void onAccountSignInError(String message);
        void onAccountStartSignIn();
        void onAccountWillCreateNew();
    }
}
