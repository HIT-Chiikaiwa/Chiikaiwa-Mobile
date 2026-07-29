import React, { useEffect, useRef, useState } from 'react';
import { ImageSourcePropType, Pressable, Text, TextInput, View } from 'react-native';
import { assets } from '../assets';
import { AuthShell, InputField, MinimalAuthScreen, PrimaryButton } from '../components/shared';
import { apiRequest } from '../services/api';
import { styles } from '../styles';
import type { BaseResponse, LoginResponse, Session, ToastState, VerifyEmailParams } from '../types';

function LoginScreen({
  onLogin,
  onRegister,
  onForgot,
  showToast,
}: {
  onLogin: (session: Session) => void;
  onRegister: () => void;
  onForgot: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('');
  const [headerImage, setHeaderImage] = useState<ImageSourcePropType>(assets.frame3);

  const submit = async () => {
    setError('');
    if (!email.trim()) {
      setError('Vui lòng nhập email');
      setHeaderImage(assets.frame2);
      return;
    }
    if (!password.trim()) {
      setError('Vui lòng nhập mật khẩu');
      setHeaderImage(assets.frame2);
      return;
    }
    setLoading(true);
    setStatus('Đang đăng nhập...');
    try {
      const response = await apiRequest<LoginResponse>('api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: email.trim(), password: password.trim() }),
      });
      onLogin({
        accessToken: response.data.accessToken,
        refreshToken: response.data.refreshToken,
        userId: response.data.id,
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Đăng nhập thất bại, vui lòng thử lại';
      setError(message);
      setHeaderImage(assets.frame2);
    } finally {
      setLoading(false);
      setStatus('');
    }
  };

  return (
    <AuthShell image={headerImage} imageHeight={180}>
      <InputField label="Email" value={email} onChangeText={setEmail} placeholder="Nhập email của bạn" keyboardType="email-address" />
      <InputField label="Mật khẩu" value={password} onChangeText={setPassword} placeholder="Nhập mật khẩu" secure />
      {status ? <Text style={styles.statusText}>{status}</Text> : null}
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
      <Pressable style={styles.forgotButton} onPress={onForgot}>
        <Text style={styles.linkText}>Quên mật khẩu?</Text>
      </Pressable>
      <PrimaryButton title="Đăng nhập" onPress={submit} loading={loading} />
      <View style={styles.authDivider}>
        <View style={styles.dividerLine} />
        <Text style={styles.dividerText}>Chưa có tài khoản</Text>
        <View style={styles.dividerLine} />
      </View>
      <PrimaryButton title="Đăng kí ngay" variant="outline" onPress={onRegister} />
    </AuthShell>
  );
}

function RegisterScreen({
  onBack,
  onRegistered,
  showToast,
}: {
  onBack: () => void;
  onRegistered: (email: string) => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [lastName, setLastName] = useState('');
  const [firstName, setFirstName] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | 'OTHER'>('MALE');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setError('');
    const checks = [
      [!lastName.trim(), 'Vui lòng nhập họ'],
      [!firstName.trim(), 'Vui lòng nhập tên'],
      [!dateOfBirth.trim(), 'Vui lòng chọn ngày sinh'],
      [!email.trim(), 'Vui lòng nhập email'],
      [!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()), 'Email không đúng định dạng'],
      [!password.trim(), 'Vui lòng nhập mật khẩu'],
      [password.length < 8, 'Mật khẩu phải có ít nhất 8 ký tự'],
      [!confirmPassword.trim(), 'Vui lòng nhập lại mật khẩu'],
      [password !== confirmPassword, 'Mật khẩu xác nhận không khớp'],
    ] as const;
    const failed = checks.find(([condition]) => condition);
    if (failed) {
      setError(failed[1]);
      return;
    }
    setLoading(true);
    try {
      const response = await apiRequest<BaseResponse<{ message?: string }>>('api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          email: email.trim(),
          password,
          confirmPassword,
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          gender,
          dateOfBirth: dateOfBirth.trim(),
        }),
      });
      showToast(response.data?.message ?? response.message ?? 'Đăng ký thành công', 'success');
      onRegistered(email.trim());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đăng ký thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell image={assets.frame4} imageHeight={100} onBack={onBack}>
      <View style={styles.rowGap}>
        <View style={styles.half}>
          <InputField label="Họ" value={lastName} onChangeText={setLastName} placeholder="Họ" />
        </View>
        <View style={styles.half}>
          <InputField label="Tên" value={firstName} onChangeText={setFirstName} placeholder="Tên" />
        </View>
      </View>
      <View style={styles.rowGap}>
        <View style={styles.half}>
          <Text style={styles.label}>Giới tính</Text>
          <View style={styles.segmented}>
            {[
              ['MALE', 'Nam'],
              ['FEMALE', 'Nữ'],
              ['OTHER', 'Khác'],
            ].map(([value, label]) => (
              <Pressable
                key={value}
                style={[styles.segment, gender === value && styles.segmentActive]}
                onPress={() => setGender(value as typeof gender)}
              >
                <Text style={[styles.segmentText, gender === value && styles.segmentTextActive]}>{label}</Text>
              </Pressable>
            ))}
          </View>
        </View>
        <View style={styles.wideHalf}>
          <InputField label="Ngày sinh" value={dateOfBirth} onChangeText={setDateOfBirth} placeholder="yyyy-MM-dd" />
        </View>
      </View>
      <InputField label="Email" value={email} onChangeText={setEmail} placeholder="Nhập email của bạn" keyboardType="email-address" />
      <InputField label="Mật khẩu" value={password} onChangeText={setPassword} placeholder="Nhập mật khẩu" secure />
      <InputField label="Nhập lại mật khẩu" value={confirmPassword} onChangeText={setConfirmPassword} placeholder="Nhập mật khẩu" secure />
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
      <PrimaryButton title="Đăng kí ngay" onPress={submit} loading={loading} />
      <Pressable onPress={onBack} style={styles.centerLink}>
        <Text style={styles.linkText}>Đã có tài khoản? Đăng nhập</Text>
      </Pressable>
    </AuthShell>
  );
}

function InputEmailScreen({
  onBack,
  onContinue,
  showToast,
}: {
  onBack: () => void;
  onContinue: (email: string) => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [email, setEmail] = useState('');
  return (
    <MinimalAuthScreen
      title="Bạn cần xác thực bảo mật!"
      description="Nhập email nhận mã"
      buttonTitle="Nhận mã OTP"
      onBack={onBack}
      onSubmit={() => {
        if (!email.trim()) {
          showToast('Vui lòng nhập email', 'error');
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
          showToast('Email không hợp lệ', 'error');
        } else {
          onContinue(email.trim());
        }
      }}
    >
      <View style={styles.minimalInput}>
        <InputField value={email} onChangeText={setEmail} placeholder="Nhập email của bạn" keyboardType="email-address" />
      </View>
    </MinimalAuthScreen>
  );
}

function VerifyEmailScreen({
  params,
  onBack,
  onSent,
  showToast,
}: {
  params: VerifyEmailParams;
  onBack: () => void;
  onSent: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    if (!params.email) {
      showToast('Không tìm thấy email', 'error');
      return;
    }
    setLoading(true);
    const endpoint =
      params.flow === 'forgot_password' ? 'api/v1/auth/forgot-password/send-otp' : 'api/v1/auth/send-otp';
    try {
      const response = await apiRequest<BaseResponse<{ message?: string }>>(endpoint, {
        method: 'POST',
        body: JSON.stringify({ email: params.email }),
      });
      showToast(response.data?.message ?? response.message ?? 'Gửi mã OTP thành công', 'success');
      onSent();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể gửi OTP', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <MinimalAuthScreen
      title="Bạn cần xác thực email!"
      description={`Nhấn Nhận mã OTP để nhận mã nhé!\nChíp sẽ gửi OTP tới ${params.email}`}
      buttonTitle="Nhận mã OTP"
      onBack={onBack}
      onSubmit={submit}
      loading={loading}
    />
  );
}

function VerifyOtpScreen({
  params,
  onBack,
  onVerified,
  showToast,
}: {
  params: VerifyEmailParams;
  onBack: () => void;
  onVerified: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [digits, setDigits] = useState(['', '', '', '', '']);
  const [cooldown, setCooldown] = useState(60);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const refs = useRef<Array<TextInput | null>>([]);

  useEffect(() => {
    const timer = setInterval(() => {
      setCooldown((current) => (current > 0 ? current - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const setDigit = (text: string, index: number) => {
    const next = [...digits];
    next[index] = text.replace(/\D/g, '').slice(-1);
    setDigits(next);
    if (next[index] && index < refs.current.length - 1) {
      refs.current[index + 1]?.focus();
    }
  };

  const verify = async () => {
    const otpCode = digits.join('');
    setError('');
    if (!otpCode) {
      setError('Vui lòng nhập mã OTP');
      return;
    }
    setLoading(true);
    const endpoint =
      params.flow === 'forgot_password'
        ? 'api/v1/auth/forgot-password/verify-otp'
        : 'api/v1/auth/verify-register-otp';
    try {
      const response = await apiRequest<BaseResponse<{ message?: string }>>(endpoint, {
        method: 'POST',
        body: JSON.stringify({ email: params.email, otpCode }),
      });
      showToast(response.data?.message ?? response.message ?? 'Xác thực OTP thành công', 'success');
      onVerified();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Xác thực OTP thất bại');
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    setError('');
    const endpoint =
      params.flow === 'forgot_password' ? 'api/v1/auth/forgot-password/send-otp' : 'api/v1/auth/send-otp';
    setLoading(true);
    try {
      const response = await apiRequest<BaseResponse<{ message?: string }>>(endpoint, {
        method: 'POST',
        body: JSON.stringify({ email: params.email }),
      });
      showToast(response.data?.message ?? response.message ?? 'Đã gửi lại mã OTP', 'success');
      setCooldown(60);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể gửi lại OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell image={assets.frame1} imageHeight={150} onBack={onBack}>
      <Text style={styles.otpTitle}>Xác thực Email</Text>
      <Text style={styles.otpDesc}>{`Mã OTP đã được gửi đến email:\n${params.email}`}</Text>
      <Text style={styles.otpPrompt}>Vui lòng nhập mã OTP</Text>
      <View style={styles.otpRow}>
        {digits.map((digit, index) => (
          <TextInput
            key={index}
            ref={(node) => {
              refs.current[index] = node;
            }}
            style={styles.otpInput}
            value={digit}
            onChangeText={(text) => setDigit(text, index)}
            keyboardType="numeric"
            maxLength={1}
          />
        ))}
      </View>
      {error ? <Text style={styles.errorTextCentered}>{error}</Text> : null}
      <PrimaryButton title="Xác thực" onPress={verify} loading={loading} />
      <PrimaryButton
        title={cooldown > 0 ? `Gửi lại (${cooldown}s)` : 'Gửi lại'}
        onPress={resend}
        variant="outline"
        disabled={cooldown > 0}
        loading={loading && cooldown === 0}
      />
    </AuthShell>
  );
}

function ResetPasswordScreen({
  email,
  onBack,
  onDone,
  showToast,
}: {
  email: string;
  onBack: () => void;
  onDone: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setError('');
    const passwordRegex = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!password) {
      setError('Vui lòng nhập mật khẩu mới');
      return;
    }
    if (!passwordRegex.test(password)) {
      setError('Mật khẩu phải bao gồm cả chữ, số và ký tự đặc biệt (tối thiểu 8 ký tự)');
      return;
    }
    if (!confirmPassword) {
      setError('Vui lòng nhập lại mật khẩu mới');
      return;
    }
    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      return;
    }
    setLoading(true);
    try {
      const response = await apiRequest<BaseResponse<{ message?: string }>>('api/v1/auth/forgot-password/reset', {
        method: 'POST',
        body: JSON.stringify({ email, password, confirmPassword }),
      });
      showToast(response.data?.message ?? response.message ?? 'Tạo mật khẩu mới thành công!', 'success');
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đổi mật khẩu thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell image={assets.frame3} imageHeight={170} onBack={onBack}>
      <Text style={styles.resetTitle}>Hãy tạo mật khẩu mới!</Text>
      <InputField label="Nhập mật khẩu mới" value={password} onChangeText={setPassword} placeholder="Nhập mật khẩu mới" secure />
      <InputField
        label="Nhập lại mật khẩu mới"
        value={confirmPassword}
        onChangeText={setConfirmPassword}
        placeholder="Nhập lại mật khẩu mới"
        secure
      />
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
      <PrimaryButton title="Xác nhận" onPress={submit} loading={loading} />
    </AuthShell>
  );
}

export { InputEmailScreen, LoginScreen, RegisterScreen, ResetPasswordScreen, VerifyEmailScreen, VerifyOtpScreen };
