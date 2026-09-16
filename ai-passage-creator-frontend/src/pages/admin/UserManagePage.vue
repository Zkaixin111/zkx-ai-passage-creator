<template>
  <div id="userManagePage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-container">
        <div class="header-content">
          <h1 class="page-title">用户管理</h1>
          <p class="page-subtitle">管理系统中的所有用户</p>
        </div>
      </div>
    </div>

    <div class="container">
      <a-card :bordered="false" class="content-card">
        <!-- 搜索 + 新增 -->
        <div class="search-section">
          <a-form layout="inline" :model="searchParams" @finish="doSearch" class="search-form">
            <a-form-item label="账号">
              <a-input
                v-model:value="searchParams.userAccount"
                placeholder="输入账号"
                allow-clear
                class="search-input"
              />
            </a-form-item>
            <a-form-item label="用户名">
              <a-input
                v-model:value="searchParams.userName"
                placeholder="输入用户名"
                allow-clear
                class="search-input"
              />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" html-type="submit">
                  <template #icon>
                    <SearchOutlined />
                  </template>
                  搜索
                </a-button>
                <a-button @click="doReset">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>

          <a-button type="primary" class="add-btn" @click="openAddModal">
            <template #icon>
              <PlusOutlined />
            </template>
            添加用户
          </a-button>
        </div>

        <a-divider />

        <!-- 表格 -->
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="pagination"
          :loading="loading"
          row-key="id"
          @change="doTableChange"
          class="user-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'userAvatar'">
              <a-avatar :src="record.userAvatar" :size="40" class="user-avatar">
                {{ (record.userName || record.userAccount || '?').charAt(0) }}
              </a-avatar>
            </template>
            <template v-else-if="column.dataIndex === 'userName'">
              <div class="name-cell">
                <span class="name-text">{{ record.userName || '-' }}</span>
                <span v-if="record.id === loginUserStore.loginUser.id" class="self-tag">
                  我
                </span>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'userProfile'">
              <a-tooltip :title="record.userProfile" placement="topLeft">
                <span class="profile-text">
                  {{ record.userProfile || '-' }}
                </span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'userRole'">
              <a-tag v-if="record.userRole === 'admin'" color="purple" class="role-tag">
                管理员
              </a-tag>
              <a-tag v-else color="blue" class="role-tag">
                普通用户
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              <span class="time-text">
                {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openEditModal(record)">
                  编辑
                </a-button>
                <a-popconfirm
                  title="确定要删除此用户吗？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="doDelete(record.id)"
                >
                  <a-button type="link" danger size="small" :disabled="record.id === loginUserStore.loginUser.id">
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 新增 / 编辑用户弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalMode === 'add' ? '添加用户' : '编辑用户'"
      :confirm-loading="modalSubmitting"
      :width="520"
      @ok="handleModalOk"
      @cancel="modalVisible = false"
    >
      <a-form
        ref="modalFormRef"
        :model="modalForm"
        :rules="modalRules"
        layout="vertical"
        class="modal-form"
      >
        <a-form-item v-if="modalMode === 'add'" label="登录账号" name="userAccount">
          <a-input
            v-model:value="modalForm.userAccount"
            placeholder="请输入登录账号"
            :maxlength="32"
          />
        </a-form-item>
        <a-form-item v-if="modalMode === 'add'" label="登录密码" name="userPassword">
          <a-input-password
            v-model:value="modalForm.userPassword"
            placeholder="请输入初始密码"
          />
        </a-form-item>
        <a-form-item label="用户昵称" name="userName">
          <a-input
            v-model:value="modalForm.userName"
            placeholder="请输入用户昵称"
            :maxlength="32"
          />
        </a-form-item>
        <a-form-item label="头像 URL" name="userAvatar">
          <a-input
            v-model:value="modalForm.userAvatar"
            placeholder="请输入头像图片链接（可选）"
          />
        </a-form-item>
        <a-form-item label="个人简介" name="userProfile">
          <a-textarea
            v-model:value="modalForm.userProfile"
            placeholder="请输入个人简介（可选）"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>
        <a-form-item label="用户角色" name="userRole">
          <a-select v-model:value="modalForm.userRole" placeholder="请选择用户角色">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  listUsers,
  deleteUser,
  addUser,
  updateUserByAdmin,
} from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { message, type FormInstance } from 'ant-design-vue'
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const loginUserStore = useLoginUserStore()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 100,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 80,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 140,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 140,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '角色',
    dataIndex: 'userRole',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    fixed: 'right' as const,
  },
]

// 表格数据
const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  userAccount: '',
})

// 拉取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUsers({
      pageNum: searchParams.pageNum,
      pageSize: searchParams.pageSize,
      userName: searchParams.userName || undefined,
      userAccount: searchParams.userAccount || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + (res.data.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格分页变化
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 重置搜索
const doReset = () => {
  searchParams.userName = ''
  searchParams.userAccount = ''
  searchParams.pageNum = 1
  fetchData()
}

// 删除（雪花 ID 为字符串）
const doDelete = async (id: string) => {
  if (!id) return
  try {
    const res = await deleteUser(id)
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败，' + (res.data.message || '未知错误'))
    }
  } catch {
    message.error('删除失败')
  }
}

// ============= 新增 / 编辑弹窗 =============
const modalVisible = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const modalSubmitting = ref(false)
const modalFormRef = ref<FormInstance>()
const editingId = ref<string>()

const modalForm = reactive<{
  userAccount?: string
  userPassword?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
}>({})

const modalRules = {
  userAccount: [
    { required: true, message: '请输入登录账号' },
    { min: 4, message: '账号长度不能小于 4 位' },
  ],
  userPassword: [
    { required: true, message: '请输入登录密码' },
    { min: 8, message: '密码长度不能小于 8 位' },
  ],
  userName: [{ required: true, message: '请输入用户昵称' }],
  userRole: [{ required: true, message: '请选择用户角色' }],
}

// 打开新增弹窗
const openAddModal = () => {
  modalMode.value = 'add'
  editingId.value = undefined
  Object.assign(modalForm, {
    userAccount: '',
    userPassword: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
  })
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: API.UserVO) => {
  modalMode.value = 'edit'
  editingId.value = record.id
  Object.assign(modalForm, {
    userAccount: record.userAccount,
    userPassword: '',
    userName: record.userName,
    userAvatar: record.userAvatar,
    userProfile: record.userProfile,
    userRole: record.userRole,
  })
  modalVisible.value = true
}

// 提交弹窗
const handleModalOk = async () => {
  try {
    await modalFormRef.value?.validate()
  } catch {
    return
  }
  modalSubmitting.value = true
  try {
    if (modalMode.value === 'add') {
      const res = await addUser({
        userAccount: modalForm.userAccount,
        userPassword: modalForm.userPassword,
        userName: modalForm.userName,
        userRole: modalForm.userRole,
      })
      if (res.data.code === 0) {
        message.success('添加成功')
        modalVisible.value = false
        fetchData()
      } else {
        message.error('添加失败，' + (res.data.message || '未知错误'))
      }
    } else {
      if (!editingId.value) return
      const res = await updateUserByAdmin(editingId.value, {
        id: editingId.value,
        userName: modalForm.userName,
        userAvatar: modalForm.userAvatar,
        userProfile: modalForm.userProfile,
        userRole: modalForm.userRole,
      })
      if (res.data.code === 0) {
        message.success('修改成功')
        modalVisible.value = false
        fetchData()
      } else {
        message.error('修改失败，' + (res.data.message || '未知错误'))
      }
    }
  } finally {
    modalSubmitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
#userManagePage {
  background: var(--color-background-secondary);
  min-height: calc(100vh - 64px);
  padding-bottom: 60px;

  .page-header {
    background: var(--gradient-hero);
    padding: 32px 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 1200px;
    margin: 0 auto;
  }

  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin: 0 0 6px;
    letter-spacing: -0.5px;
    color: var(--color-text);
  }

  .page-subtitle {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin: 0;
  }

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .content-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    box-shadow: none;
    background: white;

    :deep(.ant-card-body) {
      padding: 24px;
    }
  }

  .search-section {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 8px;
  }

  .search-form {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: flex-end;

    :deep(.ant-form-item) {
      margin-bottom: 0;
    }

    :deep(.ant-form-item-label > label) {
      font-weight: 500;
      font-size: 13px;
      color: var(--color-text-secondary);
    }
  }

  .search-input {
    width: 180px;
    border-radius: var(--radius-md);
  }

  .add-btn {
    border-radius: var(--radius-md);
    font-weight: 500;
    background: var(--gradient-primary) !important;
    border: none !important;
    color: white !important;
    box-shadow: var(--shadow-green) !important;
    transition: opacity var(--transition-normal) !important;

    &:hover,
    &:focus,
    &:active {
      background: var(--gradient-primary) !important;
      border: none !important;
      color: white !important;
      box-shadow: var(--shadow-green) !important;
      opacity: 0.92;
    }

    :deep(.ant-wave) {
      display: none;
    }
  }

  .user-table {
    :deep(.ant-table-thead > tr > th) {
      background: var(--color-background-secondary);
      font-weight: 600;
      font-size: 13px;
      color: var(--color-text-secondary);
      border-bottom: 1px solid var(--color-border);
      padding: 14px 16px;
    }

    :deep(.ant-table-tbody > tr > td) {
      padding: 14px 16px;
      border-bottom: 1px solid var(--color-border-light);
    }

    :deep(.ant-table-tbody > tr:hover > td) {
      background: rgba(180, 83, 9, 0.02);
    }

    :deep(.ant-table-pagination) {
      margin: 16px 0 0;
    }
  }

  .user-avatar {
    border: 2px solid var(--color-border);
    background: var(--gradient-primary) !important;
    color: white !important;
    font-weight: 600;
  }

  .name-cell {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .name-text {
    color: var(--color-text);
    font-weight: 500;
  }

  .self-tag {
    display: inline-block;
    padding: 0 6px;
    font-size: 11px;
    color: var(--color-primary-dark);
    background: rgba(180, 83, 9, 0.1);
    border-radius: var(--radius-sm);
  }

  .profile-text {
    color: var(--color-text-secondary);
    font-size: 13px;
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .role-tag {
    border-radius: var(--radius-full);
    font-weight: 500;
    font-size: 12px;
    padding: 2px 10px;
  }

  .time-text {
    color: var(--color-text-secondary);
    font-size: 13px;
  }

  .modal-form {
    margin-top: 8px;
  }
}

@media (max-width: 768px) {
  #userManagePage {
    .page-title {
      font-size: 22px;
    }

    .search-section {
      flex-direction: column;
    }

    .search-form {
      width: 100%;
      flex-direction: column;
      align-items: stretch;

      :deep(.ant-form-item) {
        width: 100%;
      }
    }

    .search-input {
      width: 100%;
    }

    .add-btn {
      width: 100%;
    }
  }
}
</style>
