import os
import re

base_dir = r"e:\HIT_Product\app\src\main\java\com\example\myapplication"

# 1. Update BaseViewModel
bvm_path = os.path.join(base_dir, r"ui\base\BaseViewModel.kt")
with open(bvm_path, "r", encoding="utf-8") as f:
    bvm = f.read()

bvm = bvm.replace("import androidx.lifecycle.LiveData", "import kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.asStateFlow\nimport kotlinx.coroutines.flow.SharedFlow\nimport kotlinx.coroutines.flow.MutableSharedFlow\nimport kotlinx.coroutines.flow.asSharedFlow")
bvm = bvm.replace("import androidx.lifecycle.MutableLiveData\n", "")
bvm = bvm.replace("import com.example.myapplication.utils.common.SingleLiveEvent\n", "")

bvm = bvm.replace("protected val _uiState = MutableLiveData<UiState<T>>(UiState.Idle)", "protected val _uiState = MutableStateFlow<UiState<T>>(UiState.Idle)")
bvm = bvm.replace("val uiState: LiveData<UiState<T>> = _uiState", "val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()")
bvm = bvm.replace("protected val _event = SingleLiveEvent<UiEvent>()", "protected val _event = MutableSharedFlow<UiEvent>()")
bvm = bvm.replace("val event: LiveData<UiEvent> = _event", "val event: SharedFlow<UiEvent> = _event.asSharedFlow()")

with open(bvm_path, "w", encoding="utf-8") as f:
    f.write(bvm)

# 2. Add extensions to BaseActivity and Fragment extensions
ba_path = os.path.join(base_dir, r"ui\base\BaseActivity.kt")
with open(ba_path, "r", encoding="utf-8") as f:
    ba = f.read()

ba = ba.replace("import androidx.viewbinding.ViewBinding", """import androidx.viewbinding.ViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch""")

extensions = """
    protected inline fun <T> StateFlow<T>.observeState(crossinline action: (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }

    protected inline fun <T> SharedFlow<T>.observeEvent(crossinline action: (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }
"""
ba = ba.replace("abstract fun observeData()", extensions + "\n    abstract fun observeData()")
with open(ba_path, "w", encoding="utf-8") as f:
    f.write(ba)

# Fragment extensions (for MapFragment)
frag_ext_dir = os.path.join(base_dir, r"utils\extension")
os.makedirs(frag_ext_dir, exist_ok=True)
frag_ext_path = os.path.join(frag_ext_dir, "FragmentExt.kt")
with open(frag_ext_path, "w", encoding="utf-8") as f:
    f.write("""package com.example.myapplication.utils.extension

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

inline fun <T> Fragment.observeState(stateFlow: StateFlow<T>, crossinline action: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            stateFlow.collect { action(it) }
        }
    }
}

inline fun <T> Fragment.observeEvent(sharedFlow: SharedFlow<T>, crossinline action: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sharedFlow.collect { action(it) }
        }
    }
}
""")

# 3. ViewModels - replace _event.value with emit
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith("ViewModel.kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            if "_event.value =" in content:
                if "import androidx.lifecycle.viewModelScope" not in content:
                    content = re.sub(r'(import .*?\n)', r'\1import androidx.lifecycle.viewModelScope\nimport kotlinx.coroutines.launch\n', content, count=1)
                
                # We need to handle multi-line assignments potentially, but mostly it's single line
                content = re.sub(r'_event\.value\s*=\s*(.+)', r'viewModelScope.launch { _event.emit(\1) }', content)
                with open(path, "w", encoding="utf-8") as f:
                    f.write(content)

# 4. Activities and Fragments - replace .observe(this)
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith("Activity.kt") or file.endswith("Fragment.kt") or file == "MapManager.kt":
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            # For Activities
            if "uiState.observe(this)" in content:
                content = content.replace("uiState.observe(this) {", "uiState.observeState {")
            if "event.observe(this)" in content:
                content = content.replace("event.observe(this) {", "event.observeEvent {")
                
            # For MapFragment and MapManager
            if "observe(viewLifecycleOwner)" in content:
                if "import com.example.myapplication.utils.extension.observeState" not in content:
                    content = re.sub(r'(import .*?\n)', r'\1import com.example.myapplication.utils.extension.observeState\nimport com.example.myapplication.utils.extension.observeEvent\n', content, count=1)
                
                content = re.sub(r'viewModel\.(\w+)\.observe\(viewLifecycleOwner\) \{', r'observeState(viewModel.\1) {', content)

            with open(path, "w", encoding="utf-8") as f:
                f.write(content)

# 5. Delete SingleLiveEvent
sle_path = os.path.join(base_dir, r"utils\common\SingleLiveEvent.kt")
if os.path.exists(sle_path):
    os.remove(sle_path)

print("Migration script completed!")
