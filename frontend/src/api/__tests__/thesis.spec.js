
import { describe, it, expect, vi } from 'vitest';
import { getMyTheses, createThesis, uploadVersion, getVersions, downloadVersion } from '../thesis';
import request from '../../utils/request';

vi.mock('../../utils/request');

describe('API: thesis', () => {

  it('getMyTheses should call request with correct parameters', async () => {
    const responseData = [{ id: 1, title: 'Thesis 1' }];
    request.mockResolvedValue(responseData);

    const result = await getMyTheses();

    expect(request).toHaveBeenCalledWith({
      url: '/thesis/my',
      method: 'get',
    });
    expect(result).toEqual(responseData);
  });

  it('createThesis should call request with correct parameters', async () => {
    const title = 'New Thesis Title';
    const responseData = { id: 2, title };
    request.mockResolvedValue(responseData);

    const result = await createThesis(title);

    expect(request).toHaveBeenCalledWith({
      url: '/thesis/create',
      method: 'post',
      params: { title },
    });
    expect(result).toEqual(responseData);
  });

  it('uploadVersion should call request with multipart/form-data header', async () => {
    const thesisId = 1;
    const formData = new FormData();
    formData.append('file', 'file_content');
    const responseData = { message: 'Upload successful' };
    request.mockResolvedValue(responseData);

    const result = await uploadVersion(thesisId, formData);

    expect(request).toHaveBeenCalledWith({
      url: `/thesis/${thesisId}/upload`,
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    expect(result).toEqual(responseData);
  });

  it('getVersions should call request with correct parameters', async () => {
    const thesisId = 1;
    const responseData = [{ id: 1, version: 1 }];
    request.mockResolvedValue(responseData);

    const result = await getVersions(thesisId);

    expect(request).toHaveBeenCalledWith({
      url: `/thesis/${thesisId}/versions`,
      method: 'get',
    });
    expect(result).toEqual(responseData);
  });

  it('downloadVersion should call request with blob responseType', async () => {
    const versionId = 1;
    const blobData = new Blob(['file content']);
    request.mockResolvedValue(blobData);

    const result = await downloadVersion(versionId);

    expect(request).toHaveBeenCalledWith({
      url: `/thesis/version/${versionId}/download`,
      method: 'get',
      responseType: 'blob',
    });
    expect(result).toBeInstanceOf(Blob);
  });
});
