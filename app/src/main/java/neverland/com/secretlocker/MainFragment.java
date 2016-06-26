package neverland.com.secretlocker;


import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainFragment extends Fragment {

    private static final int INTENT_RESULT_OK = 0;
    private static final int INTENT_RESULT_CANCEL = 1;
    private String mMode;
    private Uri mUri;
    private String mPath;

    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private File mFile;
    private Cipher mCipher;
    private SecretKeySpec mSecretKeySpec;
    private IvParameterSpec mIvParameterSpec;
    private CipherOutputStream mCipherOutputStream;
    private CipherInputStream mCipherInputStream;

    public static final MainFragment newInstance(String type) {
        MainFragment fragment = new MainFragment();
        Bundle bundle = new Bundle();
        bundle.putString("KEY_MODE", type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMode = getArguments().getString("KEY_MODE");
    }

    private Button.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/jpeg");
            intent.createChooser(intent, getString(R.string.intent_dialog_title));
            startActivityForResult(intent, 0);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_RESULT_OK) {
            if (data != null) {
                mUri = data.getData();
                Cursor cursor = getActivity().getContentResolver().query(mUri, null, null, null, null);
                cursor.moveToFirst();
                mPath = cursor.getString(1);
                mFile = new File(mPath);
                if (mMode.equals("Encrypt")) {
                    encrypt();
                } else if (mMode.equals("Decrypt")) {
                    decrypt();
                }
            }
        } else if (requestCode == INTENT_RESULT_CANCEL)

        {
            return;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        Button btn_encrypt = (Button) view.findViewById(R.id.btn_encrypt);
        btn_encrypt.setOnClickListener(mClick);
        Button btn_decrypt = (Button) view.findViewById(R.id.btn_decrypt);
        btn_decrypt.setOnClickListener(mClick);

        if (mMode.equals("Encrypt")) {
            btn_encrypt.setVisibility(View.VISIBLE);
        } else if (mMode.equals("Decrypt")) {
            btn_decrypt.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private void encrypt() {
        try {
            mFileInputStream = new FileInputStream(mFile);

            File encryptFile = new File(LockerMain.getFolder(), getFileName() + ".lock");
            if (!encryptFile.exists()) {
                encryptFile.createNewFile();
            }

            mFileOutputStream = new FileOutputStream(encryptFile);

            mCipher = mCipher.getInstance("AES/CBC/PKCS5Padding");
            mIvParameterSpec = new IvParameterSpec(decideKeyLength("ABC").getBytes());
            mSecretKeySpec = new SecretKeySpec(decideKeyLength("ABC").getBytes(), "AES");
            mCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, mIvParameterSpec);

            mCipherOutputStream = new CipherOutputStream(mFileOutputStream, mCipher);

            int data_byte;
            byte[] data_bytes_block = new byte[8];
            while ((data_byte = mFileInputStream.read(data_bytes_block)) != -1) {
                mCipherOutputStream.write(data_bytes_block, 0, data_byte);
            }
            mCipherOutputStream.flush();
            mCipherOutputStream.close();
            mFileInputStream.close();
            mFileOutputStream.close();

            mFile.delete();
        } catch (Exception e) {

        }
    }

    private void decrypt() {
        try {
            mFileInputStream = new FileInputStream(mFile);
            File decryptFile = new File(LockerMain.getFolder(), getFileName() + ".jpg");

            mCipher = mCipher.getInstance("AES/CBC/PKCS5Padding");
            mIvParameterSpec = new IvParameterSpec(decideKeyLength("ABC").getBytes());
            mSecretKeySpec = new SecretKeySpec(decideKeyLength("ABC").getBytes(), "AES");
            mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);

            mFileOutputStream = new FileOutputStream(decryptFile);
            mCipherInputStream = new CipherInputStream(mFileInputStream, mCipher);

            int data_byte;
            byte[] data_bytes_block = new byte[8];
            while ((data_byte = mCipherInputStream.read(data_bytes_block)) != -1) {
                mFileOutputStream.write(data_bytes_block, 0, data_byte);
            }
            mFileOutputStream.flush();
            mCipherInputStream.close();
            mFileInputStream.close();
            mFileOutputStream.close();

            mFile.delete();
        } catch (Exception e) {

        }
    }

    private String getFileName() {
        String[] path = mPath.split("/");
        String nameWithExtension = path[path.length - 1];
        String[] name = nameWithExtension.split("\\.");
        return name[0];
    }

    private String decideKeyLength(String key) {
        int length = key.length();

        if (length < 16) { //Generate 16 bits key
            for (int i = length; i < 16; ++i)
                key += i % 10;
            return key;
        } else if (length < 24) { //Generate 24 bits key
            for (int i = length; i < 24; ++i)
                key += i % 10;
            return key;
        } else if (length < 32) { //Generate 32 bits key
            for (int i = length; i < 32; ++i)
                key += i % 10;
            return key;
        }
        return key.substring(0, 32);
    }
}
