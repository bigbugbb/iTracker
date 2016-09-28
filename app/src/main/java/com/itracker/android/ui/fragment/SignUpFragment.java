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

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.model.User;
import com.itracker.android.ui.activity.AuthenticatorActivity;
import com.itracker.android.ui.listener.OnAuthStateChangedListener;
import com.itracker.android.utils.AccountUtils;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SignUpFragment.OnAccountSignUpListener} interface
 * to handle interaction events.
 */
public class SignUpFragment extends Fragment implements OnAuthStateChangedListener {

    private static final String TAG = makeLogTag(SignUpFragment.class);

    private static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    private static final int MIN_PASSWORD_LENGTH = 8;

    private OnAccountSignUpListener mListener;

    private Button   mSignUp;
    private TextView mAccountExists;
    private EditText mEmail;
    private EditText mUsername;
    private EditText mPassword;
    private EditText mPasswordConfirmation;
    private TextInputLayout mEmailInputLayout;
    private TextInputLayout mUsernameInputLayout;
    private TextInputLayout mPasswordInputLayout;
    private TextInputLayout mPasswordConfirmationInputLayout;

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

        mEmail = (EditText) view.findViewById(R.id.edit_email);
        mUsername = (EditText) view.findViewById(R.id.edit_username);
        mPassword = (EditText) view.findViewById(R.id.edit_password);
        mPasswordConfirmation = (EditText) view.findViewById(R.id.password_confirmation);

        mEmailInputLayout = (TextInputLayout) view.findViewById(R.id.input_email);
        mUsernameInputLayout = (TextInputLayout) view.findViewById(R.id.input_username);
        mPasswordInputLayout = (TextInputLayout) view.findViewById(R.id.input_password);
        mPasswordConfirmationInputLayout = (TextInputLayout) view.findViewById(R.id.input_password_confirmation);

        mEmail.addTextChangedListener(new AuthTextWatcher(mEmail));
        mUsername.addTextChangedListener(new AuthTextWatcher(mPassword));
        mPassword.addTextChangedListener(new AuthTextWatcher(mEmail));
        mPasswordConfirmation.addTextChangedListener(new AuthTextWatcher(mPassword));

        mSignUp = (Button) view.findViewById(R.id.sign_up);
        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableUi(false);
                if (mListener != null) {
                    mListener.onAccountStartSignUp();
                }

                signUp();
            }
        });
        mAccountExists = (TextView) view.findViewById(R.id.account_exists);
        mAccountExists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onAccountAreadyExists();
                }
            }
        });

        final String accountType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_ACCOUNT_TYPE);
        final String authTokenType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_AUTHTOKEN_TYPE);
        AccountUtils.setAccountType(Application.getInstance(), accountType);
        AccountUtils.setAuthTokenType(Application.getInstance(), authTokenType);
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

    @Override
    public void onStart() {
        super.onStart();
        Application.getInstance().addUIListener(OnAuthStateChangedListener.class, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Application.getInstance().removeUIListener(OnAuthStateChangedListener.class, this);
    }

    private void signUp() {
        // Validation!
        final String email = mEmail.getText().toString().trim();
        final String username = mUsername.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        final String passwordConfirmation = mPasswordConfirmation.getText().toString().trim();
        final String accountType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_ACCOUNT_TYPE);

        if (!validateEmail(email) || !validateUsername(username) ||
                !validatePassword(password) || !validatePasswordConfirmation(passwordConfirmation)) {
            enableUi(true);
            if (mListener != null) {
                mListener.onAccountSignUpError("Invalid input, please try again");
            }
            return;
        }

        new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {
                LOGD(TAG, "started authenticating");

                Bundle data = new Bundle();
                try {
                    User user = AccountUtils.signUpUser(email, username, password, passwordConfirmation);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, email);
                    data.putString(AuthenticatorActivity.USERNAME, user.username);
                    data.putBoolean(AuthenticatorActivity.CREATE_ACCOUNT, true);
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
                    if (mListener != null) {
                        mListener.onAccountSignUpError(intent.getStringExtra(KEY_ERROR_MESSAGE));
                    }
                    enableUi(true);
                } else {
                    if (mListener != null) {
                        mListener.onAccountSignUpSuccess(intent);
                    }
                }
            }
        }.execute();
    }

    private void enableUi(boolean enabled) {
        mEmail.setEnabled(enabled);
        mUsername.setEnabled(enabled);
        mPassword.setEnabled(enabled);
        mPasswordConfirmation.setEnabled(enabled);
        mSignUp.setEnabled(enabled);
        mSignUp.setText(enabled ? R.string.sign_up : R.string.creating_new_account);
        mAccountExists.setEnabled(enabled);
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

    private boolean validateUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            mUsernameInputLayout.setError(getString(R.string.invalid_username));
            requestFocus(mUsername);
            return false;
        } else {
            mUsernameInputLayout.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            mPasswordInputLayout.setError(getString(R.string.invalid_password));
            requestFocus(mPassword);
            return false;
        } else if (TextUtils.getTrimmedLength(password) < MIN_PASSWORD_LENGTH) {
            mPasswordInputLayout.setError(getString(R.string.invalid_password_length));
            requestFocus(mPassword);
            return false;
        } else {
            mPasswordInputLayout.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePasswordConfirmation(String passwordConfirmation) {
        final String password = mPassword.getText().toString().trim();
        if (TextUtils.isEmpty(passwordConfirmation)) {
            mPasswordConfirmationInputLayout.setError(getString(R.string.invalid_password_confirmation));
            requestFocus(mPasswordConfirmation);
            return false;
        } else if (!TextUtils.equals(passwordConfirmation, password)) {
            mPasswordConfirmationInputLayout.setError(getString(R.string.invalid_password_confirmation2));
            requestFocus(mPasswordConfirmation);
            return false;
        } else {
            mPasswordConfirmationInputLayout.setErrorEnabled(false);
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

    @Override
    public void onAuthStateChanged(AuthState state, Bundle extra) {

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
                case R.id.input_username:
                    validateUsername(editable.toString().trim());
                    break;
                case R.id.input_password:
                    validatePassword(editable.toString().trim());
                    break;
                case R.id.input_password_confirmation:
                    validatePasswordConfirmation(editable.toString().trim());
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
    public interface OnAccountSignUpListener {
        void onAccountSignUpSuccess(Intent intent);
        void onAccountSignUpError(String message);
        void onAccountStartSignUp();
        void onAccountAreadyExists();
    }
}
