package com.itracker.android.ui.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.LogManager;
import com.itracker.android.data.extension.avatar.AvatarManager;
import com.itracker.android.data.extension.file.FileManager;
import com.itracker.android.data.extension.vcard.OnVCardListener;
import com.itracker.android.data.extension.vcard.OnVCardSaveListener;
import com.itracker.android.data.extension.vcard.VCardManager;
import com.itracker.android.ui.activity.ChatViewerActivity;
import com.itracker.android.ui.helper.PermissionsRequester;
import com.itracker.android.xmpp.address.Jid;
import com.itracker.android.xmpp.vcard.AddressProperty;
import com.itracker.android.xmpp.vcard.TelephoneType;
import com.itracker.android.xmpp.vcard.VCardProperty;
import com.soundcloud.android.crop.Crop;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class AccountInfoEditorFragment extends Fragment implements 
        OnVCardSaveListener, OnVCardListener, DatePickerDialog.OnDateSetListener, TextWatcher {

    public static final String ARGUMENT_ACCOUNT = "com.itracker.android.ui.fragment.AccountInfoEditorFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_VCARD = "com.itracker.android.ui.fragment.AccountInfoEditorFragment.ARGUMENT_USER";
    public static final String SAVE_NEW_AVATAR_IMAGE_URI = "com.itracker.android.ui.fragment.AccountInfoEditorFragment.SAVE_NEW_AVATAR_IMAGE_URI";
    public static final String SAVE_PHOTO_FILE_URI = "com.itracker.android.ui.fragment.AccountInfoEditorFragment.SAVE_PHOTO_FILE_URI";
    public static final String SAVE_REMOVE_AVATAR_FLAG = "com.itracker.android.ui.fragment.AccountInfoEditorFragment.SAVE_REMOVE_AVATAR_FLAG";

    public static final int REQUEST_NEED_VCARD = 2;
    public static final int REQUEST_TAKE_PHOTO = 3;
    private static final int REQUEST_PERMISSION_GALLERY = 4;
    private static final int REQUEST_PERMISSION_CAMERA = 5;


    public static final int MAX_AVATAR_SIZE_PIXELS = 192;
    public static final String TEMP_FILE_NAME = "cropped";
    public static final String ROTATE_FILE_NAME = "rotated";
    public static final int KB_SIZE_IN_BYTES = 1024;
    public static final String DATE_FORMAT = "yyyy-mm-dd";
    public static final String DATE_FORMAT_INT_TO_STRING = "%d-%02d-%02d";
    public static final int MAX_IMAGE_SIZE = 512;

    private VCard mVCard;
    private String mAccount;
    private View mProgressBar;
    private boolean mIsSaveSuccess;
    private Listener mListener;
    private boolean mUpdateFromVCardFlag = true;

    private LinearLayout mFields;

    private EditText mGivenName;
    private EditText mMiddleName;
    private EditText mFamilyName;
    private EditText mNickName;

    private EditText mTitle;

    private EditText mDescription;
    private EditText mEmail;
    private EditText mPhone;

    private EditText mAddressHomePostStreet;
    private EditText mAddressHomeLocality;
    private EditText mAddressHomeRegion;
    private EditText mAddressHomeCountry;
    private EditText mAddressHomePostalCode;

    private ImageView mAvatar;
    private TextView mAvatarSize;
    private Uri mNewAvatarImageUri;
    private Uri mPhotoFileUri;
    private boolean mRemoveAvatarFlag = false;

    private TextView mBirthDate;
    private DatePickerDialog mDatePicker;
    private View mBirthDateRemoveButton;

    public interface Listener {
        void onProgressModeStarted(String message);
        void onProgressModeFinished();
        void enableSave();
    }

    public static AccountInfoEditorFragment newInstance(String account, String vCard) {
        AccountInfoEditorFragment fragment = new AccountInfoEditorFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_VCARD, vCard);
        fragment.setArguments(arguments);
        return fragment;
    }

    public AccountInfoEditorFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (Listener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mAccount = args.getString(ARGUMENT_ACCOUNT, null);
        String vCardString = args.getString(ARGUMENT_VCARD, null);
        if (vCardString != null) {
            try {
                mVCard = ContactVCardViewerFragment.parseVCard(vCardString);
            } catch (XmlPullParserException | IOException | SmackException e) {
                LogManager.exception(this, e);
            }
        }

        if (savedInstanceState != null) {
            final String avatarImageUriString = savedInstanceState.getString(SAVE_NEW_AVATAR_IMAGE_URI);
            if (avatarImageUriString != null) {
                mNewAvatarImageUri = Uri.parse(avatarImageUriString);
            }

            final String photoFileUriString = savedInstanceState.getString(SAVE_PHOTO_FILE_URI);
            if (photoFileUriString != null) {
                mPhotoFileUri = Uri.parse(photoFileUriString);
            }

            mRemoveAvatarFlag = savedInstanceState.getBoolean(SAVE_REMOVE_AVATAR_FLAG);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_info_editor, container, false);

        mFields = (LinearLayout) view.findViewById(R.id.vcard_fields_layout);
        mProgressBar = view.findViewById(R.id.vcard_save_progress_bar);

        mGivenName = setUpInputField(view, R.id.vcard_given_name);
        mMiddleName = setUpInputField(view, R.id.vcard_middle_name);
        mFamilyName = setUpInputField(view, R.id.vcard_family_name);
        mNickName = setUpInputField(view, R.id.vcard_nickname);

        mAvatar = (ImageView) view.findViewById(R.id.vcard_avatar);
        mAvatarSize = (TextView) view.findViewById(R.id.vcard_avatar_size_text_view);
        mAvatar.setOnClickListener(v -> changeAvatar());

        mBirthDate = (TextView) view.findViewById(R.id.vcard_birth_date);
        mBirthDate.setOnClickListener(v -> mDatePicker.show());
        mBirthDate.addTextChangedListener(this);

        mBirthDateRemoveButton = view.findViewById(R.id.vcard_birth_date_remove_button);
        mBirthDateRemoveButton.setOnClickListener(v -> setBirthDate(null));

        mTitle = setUpInputField(view, R.id.vcard_title);

        mDescription = setUpInputField(view, R.id.vcard_decsription);
        mPhone = setUpInputField(view, R.id.vcard_phone);
        mEmail = setUpInputField(view, R.id.vcard_email);

        mAddressHomePostStreet = setUpInputField(view, R.id.vcard_address_home_post_street);
        mAddressHomeLocality = setUpInputField(view, R.id.vcard_address_home_locality);
        mAddressHomeRegion = setUpInputField(view, R.id.vcard_address_home_region);
        mAddressHomeCountry = setUpInputField(view, R.id.vcard_address_home_country);
        mAddressHomePostalCode = setUpInputField(view, R.id.vcard_address_home_postal_code);

        setFieldsFromVCard();

        return view;
    }

    private EditText setUpInputField(View rootView, int resourceId) {
        EditText inputField = (EditText) rootView.findViewById(resourceId);
        inputField.addTextChangedListener(this);
        return inputField;
    }

    @Override
    public void onResume() {
        super.onResume();

        Application.getInstance().addUIListener(OnVCardSaveListener.class, this);
        Application.getInstance().addUIListener(OnVCardListener.class, this);

        VCardManager vCardManager = VCardManager.getInstance();
        if (vCardManager.isVCardRequested(mAccount) || vCardManager.isVCardSaveRequested(mAccount)) {
            enableProgressMode(getString(R.string.saving));
        }
        mUpdateFromVCardFlag = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnVCardSaveListener.class, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNewAvatarImageUri != null) {
            outState.putString(SAVE_NEW_AVATAR_IMAGE_URI, mNewAvatarImageUri.toString());
        }
        if (mPhotoFileUri != null) {
            outState.putString(SAVE_PHOTO_FILE_URI, mPhotoFileUri.toString());
        }
        outState.putBoolean(SAVE_REMOVE_AVATAR_FLAG, mRemoveAvatarFlag);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void setFieldsFromVCard() {
        mGivenName.setText(mVCard.getFirstName());
        mMiddleName.setText(mVCard.getMiddleName());
        mFamilyName.setText(mVCard.getLastName());
        mNickName.setText(mVCard.getNickName());

        setUpAvatarView();

        setBirthDate(mVCard.getField(VCardProperty.BDAY.name()));

        updateDatePickerDialog();

        mTitle.setText(mVCard.getField(VCardProperty.TITLE.name()));

        mDescription.setText(mVCard.getField(VCardProperty.DESC.name()));

        for (TelephoneType telephoneType : TelephoneType.values() ) {
            String phone = mVCard.getPhoneHome(telephoneType.name());
            if (phone != null && !phone.isEmpty()) {
                mPhone.setText(phone);
            }
        }

        mEmail.setText(mVCard.getEmailHome());

        mAddressHomePostStreet.setText(mVCard.getAddressFieldHome(AddressProperty.STREET.name()));
        mAddressHomeLocality.setText(mVCard.getAddressFieldHome(AddressProperty.LOCALITY.name()));
        mAddressHomeRegion.setText(mVCard.getAddressFieldHome(AddressProperty.REGION.name()));
        mAddressHomePostalCode.setText(mVCard.getAddressFieldHome(AddressProperty.PCODE.name()));
        mAddressHomeCountry.setText(mVCard.getAddressFieldHome(AddressProperty.CTRY.name()));
    }

    public void updateDatePickerDialog() {
        Calendar calendar = null;

        String vCardBirthDate = mVCard.getField(VCardProperty.BDAY.name());

        if (vCardBirthDate != null) {

            DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            Date result = null;
            try {
                result = dateFormat.parse(vCardBirthDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (result != null) {
                calendar = new GregorianCalendar();
                calendar.setTime(result);
            }
        }

        if (calendar == null) {
            calendar = Calendar.getInstance(TimeZone.getDefault());
        }
        mDatePicker = new DatePickerDialog(getActivity(),
                AccountInfoEditorFragment.this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        mDatePicker.setCancelable(false);
    }

    private void changeAvatar() {
        PopupMenu menu = new PopupMenu(getActivity(), mAvatar);
        menu.inflate(R.menu.menu_change_avatar);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_choose_from_gallery:
                    onChooseFromGalleryClick();
                    return true;
                case R.id.action_take_photo:
                    onTakePhotoClick();
                    return true;
                case R.id.action_remove_avatar:
                    removeAvatar();
                    return true;
                default:
                    return false;
            }

        });
        menu.show();
    }

    private void onTakePhotoClick() {
        if (PermissionsRequester.requestCameraPermissionIfNeeded(this, REQUEST_PERMISSION_CAMERA)) {
            takePhoto();
        }
    }

    private void onChooseFromGalleryClick() {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, REQUEST_PERMISSION_GALLERY)) {
            chooseFromGallery();
        }
    }

    private void chooseFromGallery() {
        Crop.pickImage(getActivity());
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = FileManager.createTempImageFile(TEMP_FILE_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (imageFile != null) {
                mPhotoFileUri = Uri.fromFile(imageFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoFileUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void removeAvatar() {
        mNewAvatarImageUri = null;
        mRemoveAvatarFlag = true;
        setUpAvatarView();
        mListener.enableSave();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_GALLERY:
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    chooseFromGallery();
                } else {
                    Toast.makeText(getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_PERMISSION_CAMERA:
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    takePhoto();
                } else {
                    Toast.makeText(getActivity(), R.string.no_permission_camera, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == Activity.RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            beginCrop(mPhotoFileUri);
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
    }

    private void beginCrop(final Uri source) {
        mNewAvatarImageUri = Uri.fromFile(new File(getActivity().getCacheDir(), TEMP_FILE_NAME));

        Application.getInstance().runInBackground(() -> {
            final boolean isImageNeedPreprocess = FileManager.isImageSizeGreater(source, MAX_IMAGE_SIZE)
                    || FileManager.isImageNeedRotation(source);

            Application.getInstance().runOnUiThread(() -> {
                if (isImageNeedPreprocess) {
                    preprocessAndStartCrop(source);
                } else {
                    startImageCropActivity(source);
                }
            });
        });
    }

    private void preprocessAndStartCrop(Uri source) {
        enableProgressMode(getString(R.string.processing_image));
        Glide.with(this).load(source).asBitmap().toBytes().override(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE).into(new SimpleTarget<byte[]>() {
            @Override
            public void onResourceReady(final byte[] data, GlideAnimation anim) {
                Application.getInstance().runInBackground(() -> {
                    final Uri rotatedImage = FileManager.saveImage(data, ROTATE_FILE_NAME);
                    if (rotatedImage == null) return;

                    Application.getInstance().runOnUiThread(() -> {
                        startImageCropActivity(rotatedImage);
                        disableProgressMode();
                    });
                });
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                super.onLoadFailed(e, errorDrawable);
                disableProgressMode();
                Toast.makeText(getActivity(), R.string.error_during_image_processing, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startImageCropActivity(Uri srcUri) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Crop.of(srcUri, mNewAvatarImageUri).withMaxSize(MAX_AVATAR_SIZE_PIXELS, MAX_AVATAR_SIZE_PIXELS).start(activity);
    }

    private void handleCrop(int resultCode, Intent result) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                setUpAvatarView();
                break;
            case Crop.RESULT_ERROR:
                mAvatarSize.setVisibility(View.INVISIBLE);
                Toast.makeText(getActivity(), R.string.error_during_crop, Toast.LENGTH_SHORT).show();
                // no break!
            default:
                mNewAvatarImageUri = null;
        }
    }

    private void setUpAvatarView() {
        if (mNewAvatarImageUri != null) {
            // null prompts image view to reload file.
            mAvatar.setImageURI(null);
            mAvatar.setImageURI(mNewAvatarImageUri);
            mRemoveAvatarFlag = false;

            File file = new File(mNewAvatarImageUri.getPath());
            mAvatarSize.setText(file.length() / KB_SIZE_IN_BYTES + "KB");
            mAvatarSize.setVisibility(View.VISIBLE);
            if (mListener != null) {
                mListener.enableSave();
            }
        } else if (mRemoveAvatarFlag) {
            mAvatar.setImageDrawable(AvatarManager.getInstance().getDefaultAccountAvatar(mAccount));
            mAvatarSize.setVisibility(View.INVISIBLE);
        } else {
            mAvatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(mAccount));
            mAvatarSize.setVisibility(View.INVISIBLE);
        }
    }

    String getValueFromEditText(TextView editText) {
        String trimText = editText.getText().toString().trim();
        if (trimText.isEmpty()) {
            return null;
        }

        return trimText;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void updateVCardFromFields() {

        mVCard.setFirstName(getValueFromEditText(mGivenName));
        mVCard.setMiddleName(getValueFromEditText(mMiddleName));
        mVCard.setLastName(getValueFromEditText(mFamilyName));
        mVCard.setNickName(getValueFromEditText(mNickName));

        if (mRemoveAvatarFlag) {
            mVCard.removeAvatar();
        } else if (mNewAvatarImageUri != null) {
            try {
                mVCard.setAvatar(new URL(mNewAvatarImageUri.toString()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        mVCard.setField(VCardProperty.BDAY.name(), getValueFromEditText(mBirthDate));

        mVCard.setField(VCardProperty.TITLE.name(), getValueFromEditText(mTitle));
        mVCard.setField(VCardProperty.DESC.name(), getValueFromEditText(mDescription));

        mVCard.setPhoneHome(TelephoneType.VOICE.name(), getValueFromEditText(mPhone));
        mVCard.setEmailHome(getValueFromEditText(mEmail));

        mVCard.setAddressFieldHome(AddressProperty.STREET.name(), getValueFromEditText(mAddressHomePostStreet));
        mVCard.setAddressFieldHome(AddressProperty.LOCALITY.name(), getValueFromEditText(mAddressHomeLocality));
        mVCard.setAddressFieldHome(AddressProperty.REGION.name(), getValueFromEditText(mAddressHomeRegion));
        mVCard.setAddressFieldHome(AddressProperty.PCODE.name(), getValueFromEditText(mAddressHomePostalCode));
        mVCard.setAddressFieldHome(AddressProperty.CTRY.name(), getValueFromEditText(mAddressHomeCountry));
    }

    public void saveVCard() {
        ChatViewerActivity.hideKeyboard(getActivity());
        updateVCardFromFields();
        enableProgressMode(getString(R.string.saving));
        VCardManager.getInstance().saveVCard(mAccount, mVCard);
        mIsSaveSuccess = false;
    }

    public void enableProgressMode(String message) {
        setEnabledRecursive(false, mFields);
        mProgressBar.setVisibility(View.VISIBLE);
        if (mListener != null) {
            mListener.onProgressModeStarted(message);
        }
    }

    public void disableProgressMode() {
        mProgressBar.setVisibility(View.GONE);
        setEnabledRecursive(true, mFields);
        if (mListener != null) {
            mListener.onProgressModeFinished();
        }
    }

    private void setEnabledRecursive(boolean enabled, ViewGroup viewGroup){
        for (int i = 0; i < viewGroup.getChildCount(); i++){
            View child = viewGroup.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup){
                setEnabledRecursive(enabled, (ViewGroup) child);
            }
        }
    }

    @Override
    public void onVCardSaveSuccess(String account) {
        if (!Jid.getBareAddress(mAccount).equals(Jid.getBareAddress(account))) {
            return;
        }

        enableProgressMode(getString(R.string.saving));
        VCardManager.getInstance().request(account, account);
        mIsSaveSuccess = true;
    }

    @Override
    public void onVCardSaveFailed(String account) {
        if (!Jid.getBareAddress(this.mAccount).equals(Jid.getBareAddress(account))) {
            return;
        }

        disableProgressMode();
        mListener.enableSave();
        Toast.makeText(getActivity(), getString(R.string.account_user_info_save_fail), Toast.LENGTH_LONG).show();
        mIsSaveSuccess = false;
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        if (!Jid.getBareAddress(this.mAccount).equals(Jid.getBareAddress(bareAddress))) {
            return;
        }

        if (mIsSaveSuccess) {
            Toast.makeText(getActivity(), getString(R.string.account_user_info_save_success), Toast.LENGTH_LONG).show();
            mIsSaveSuccess = false;

            Intent data = new Intent();
            data.putExtra(ARGUMENT_VCARD, vCard.getChildElementXML().toString());
            getActivity().setResult(Activity.RESULT_OK, data);

            getActivity().finish();
        } else {
            disableProgressMode();
            this.mVCard = vCard;
            mUpdateFromVCardFlag = true;
            setFieldsFromVCard();
            mUpdateFromVCardFlag = false;
        }
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {
        if (!Jid.getBareAddress(this.mAccount).equals(Jid.getBareAddress(bareAddress))) {
            return;
        }

        if (mIsSaveSuccess) {
            Toast.makeText(getActivity(), getString(R.string.account_user_info_save_success), Toast.LENGTH_LONG).show();
            mIsSaveSuccess = false;
            getActivity().setResult(REQUEST_NEED_VCARD);
            getActivity().finish();
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        setBirthDate(String.format(DATE_FORMAT_INT_TO_STRING, year, monthOfYear + 1, dayOfMonth));
    }

    public void setBirthDate(String date) {
        mBirthDate.setText(date);
        if (date == null) {
            mBirthDateRemoveButton.setVisibility(View.INVISIBLE);
        } else {
            mBirthDateRemoveButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!mUpdateFromVCardFlag && mListener != null) {
            mListener.enableSave();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}