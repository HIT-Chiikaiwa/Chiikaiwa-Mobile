import os
import re

base_dir = r"e:\HIT_Product\app\src\main\java\com\example\myapplication"

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith("ViewModel.kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            if "MutableLiveData" in content or "LiveData" in content:
                # Add imports if missing
                if "import kotlinx.coroutines.flow.MutableStateFlow" not in content:
                    content = re.sub(r'(import .*?\n)', r'\1import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\n', content, count=1)
                
                content = content.replace("import androidx.lifecycle.LiveData\n", "")
                content = content.replace("import androidx.lifecycle.MutableLiveData\n", "")

                # MapViewModel fixes
                content = content.replace("private val _nearbyUsers = MutableLiveData<List<NearbyUserResponse>>()", "private val _nearbyUsers = MutableStateFlow<List<NearbyUserResponse>>(emptyList())")
                content = content.replace("val nearbyUsers: LiveData<List<NearbyUserResponse>> get() = _nearbyUsers", "val nearbyUsers: StateFlow<List<NearbyUserResponse>> get() = _nearbyUsers")
                
                content = content.replace("private val _avatarBitmaps = MutableLiveData<Map<String, Bitmap>>(emptyMap())", "private val _avatarBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())")
                content = content.replace("val avatarBitmaps: LiveData<Map<String, Bitmap>> get() = _avatarBitmaps", "val avatarBitmaps: StateFlow<Map<String, Bitmap>> get() = _avatarBitmaps")
                
                content = content.replace("private val _currentUserAvatar = MutableLiveData<String?>()", "private val _currentUserAvatar = MutableStateFlow<String?>(null)")
                content = content.replace("val currentUserAvatar: LiveData<String?> get() = _currentUserAvatar", "val currentUserAvatar: StateFlow<String?> get() = _currentUserAvatar")

                # VerifyOtpViewModel fixes
                content = content.replace("private val _resendCooldown = MutableLiveData<Int>(0)", "private val _resendCooldown = MutableStateFlow<Int>(0)")
                content = content.replace("val resendCooldown: LiveData<Int> get() = _resendCooldown", "val resendCooldown: StateFlow<Int> get() = _resendCooldown")

                # ProfileViewModel fixes
                content = content.replace("private val _subjects = MutableLiveData<List<SubjectDto>>(emptyList())", "private val _subjects = MutableStateFlow<List<SubjectDto>>(emptyList())")
                content = content.replace("val subjects: LiveData<List<SubjectDto>> get() = _subjects", "val subjects: StateFlow<List<SubjectDto>> get() = _subjects")

                with open(path, "w", encoding="utf-8") as f:
                    f.write(content)

print("ViewModel LiveData fix completed!")
