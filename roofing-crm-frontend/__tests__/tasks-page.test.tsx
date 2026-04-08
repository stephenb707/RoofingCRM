import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import TasksPage from "@/app/app/tasks/page";
import * as tasksApi from "@/lib/tasksApi";
import { PageResponse, TaskDto } from "@/lib/types";

const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  usePathname: () => "/app/tasks",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
}));

jest.mock("@/lib/tasksApi");
const mockedTasksApi = tasksApi as jest.Mocked<typeof tasksApi>;

const mockTask: TaskDto = {
  taskId: "task-1",
  title: "Follow up call",
  status: "TODO",
  priority: "HIGH",
  dueAt: null,
  assignedToName: "Sam",
  leadId: "lead-99",
  jobId: null,
  customerId: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-02T00:00:00Z",
};

const mockPage: PageResponse<TaskDto> = {
  content: [mockTask],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe("TasksPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockedTasksApi.listTasks.mockResolvedValue(mockPage);
  });

  it("navigates to task detail when clicking the row", async () => {
    render(<TasksPage />);

    await waitFor(() => {
      expect(screen.getByText("Follow up call")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText("Open task: Follow up call"));

    expect(mockPush).toHaveBeenCalledWith("/app/tasks/task-1");
  });

  it("does not navigate to task when clicking a related entity link", async () => {
    render(<TasksPage />);

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Lead" })).toBeInTheDocument();
    });

    mockPush.mockClear();
    fireEvent.click(screen.getByRole("link", { name: "Lead" }));

    expect(mockPush).not.toHaveBeenCalledWith("/app/tasks/task-1");
  });

  it("does not fire row navigation when clicking the View link", async () => {
    render(<TasksPage />);

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "View" })).toBeInTheDocument();
    });

    mockPush.mockClear();
    fireEvent.click(screen.getByRole("link", { name: "View" }));

    expect(mockPush).not.toHaveBeenCalled();
  });
});
